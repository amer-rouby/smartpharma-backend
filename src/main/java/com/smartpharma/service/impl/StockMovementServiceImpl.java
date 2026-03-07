package com.smartpharma.service.impl;

import com.smartpharma.dto.request.StockMovementRequest;
import com.smartpharma.dto.response.StockMovementResponse;
import com.smartpharma.dto.response.StockMovementStats;
import com.smartpharma.entity.StockBatch;
import com.smartpharma.entity.StockMovement;
import com.smartpharma.entity.User;
import com.smartpharma.repository.StockBatchRepository;
import com.smartpharma.repository.StockMovementRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockMovementServiceImpl implements StockMovementService {

    private final StockMovementRepository movementRepository;
    private final StockBatchRepository batchRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public StockMovementResponse createMovement(StockMovementRequest request, Long userId) {
        StockBatch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new RuntimeException("Batch not found: " + request.getBatchId()));

        int quantityBefore = batch.getQuantityCurrent();
        int quantityAfter;

        switch (request.getMovementType()) {
            case STOCK_IN, TRANSFER_IN -> quantityAfter = quantityBefore + request.getQuantity();
            case STOCK_OUT, TRANSFER_OUT, EXPIRED, DISCARDED -> {
                if (quantityBefore < request.getQuantity()) {
                    throw new RuntimeException("Insufficient stock. Available: " + quantityBefore);
                }
                quantityAfter = quantityBefore - request.getQuantity();
            }
            case STOCK_ADJUSTMENT -> quantityAfter = request.getQuantity();
            default -> throw new IllegalArgumentException("Invalid movement type: " + request.getMovementType());
        }

        batch.setQuantityCurrent(quantityAfter);
        batchRepository.save(batch);

        BigDecimal totalValue = request.getUnitPrice() != null ?
                request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())) : null;

        // ✅ ✅ ✅ الحل: التعامل مع userId == null ✅ ✅ ✅
        User userRef = null;
        if (userId != null) {
            try {
                userRef = userRepository.getReferenceById(userId);
            } catch (Exception e) {
                log.warn("Could not load user reference for userId: {}. Proceeding without user association.", userId);
            }
        }

        StockMovement movement = StockMovement.builder()
                .batch(batch)
                .movementType(request.getMovementType())
                .quantity(request.getQuantity())
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .unitPrice(request.getUnitPrice())
                .totalValue(totalValue)
                .referenceNumber(request.getReferenceNumber())
                .reason(request.getReason())
                .notes(request.getNotes())
                .user(userRef)
                .pharmacyId(batch.getPharmacy().getId())
                .build();

        movementRepository.save(movement);
        log.info("Stock movement created: type={}, batch={}, qty={} -> {}, user={}",
                request.getMovementType(), batch.getId(), quantityBefore, quantityAfter, userId);

        return StockMovementResponse.fromEntity(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovementsByPharmacy(Long pharmacyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return movementRepository.findByPharmacyId(pharmacyId, pageable)
                .map(StockMovementResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovementsByBatch(Long batchId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return movementRepository.findByBatchId(batchId, pageable)
                .map(StockMovementResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovementsByDateRange(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return movementRepository.findByPharmacyIdAndDateRange(pharmacyId, startDate, endDate, pageable)
                .map(StockMovementResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public StockMovementStats getMovementStats(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Calculating stats for pharmacy: {} from {} to {}", pharmacyId, startDate, endDate);

        Long totalMovements = 0L;
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(pharmacyId, StockMovement.MovementType.STOCK_IN, startDate, endDate);
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(pharmacyId, StockMovement.MovementType.STOCK_OUT, startDate, endDate);
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(pharmacyId, StockMovement.MovementType.STOCK_ADJUSTMENT, startDate, endDate);
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(pharmacyId, StockMovement.MovementType.TRANSFER_IN, startDate, endDate);
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(pharmacyId, StockMovement.MovementType.TRANSFER_OUT, startDate, endDate);
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(pharmacyId, StockMovement.MovementType.EXPIRED, startDate, endDate);
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(pharmacyId, StockMovement.MovementType.DISCARDED, startDate, endDate);

        Integer totalStockIn = movementRepository.sumStockInByDateRange(pharmacyId, startDate, endDate);
        Integer totalStockOut = movementRepository.sumStockOutByDateRange(pharmacyId, startDate, endDate);

        Integer totalAdjustments = movementRepository.sumByTypeAndDateRange(pharmacyId, StockMovement.MovementType.STOCK_ADJUSTMENT, startDate, endDate);
        Integer totalExpired = movementRepository.sumByTypeAndDateRange(pharmacyId, StockMovement.MovementType.EXPIRED, startDate, endDate);
        Integer totalTransferred = movementRepository.sumByTypeAndDateRange(pharmacyId, StockMovement.MovementType.TRANSFER_IN, startDate, endDate);

        StockMovementStats stats = StockMovementStats.builder()
                .totalMovements(totalMovements)
                .totalStockIn(totalStockIn != null ? totalStockIn : 0)
                .totalStockOut(totalStockOut != null ? totalStockOut : 0)
                .totalAdjustments(totalAdjustments != null ? totalAdjustments : 0)
                .totalExpired(totalExpired != null ? totalExpired : 0)
                .totalTransferred(totalTransferred != null ? totalTransferred : 0)
                .build();

        log.info("Stats calculated: totalMovements={}, totalStockIn={}, totalStockOut={}",
                stats.getTotalMovements(), stats.getTotalStockIn(), stats.getTotalStockOut());

        return stats;
    }
    @Override
    @Transactional
    public void createStockInMovement(Long batchId, Integer quantity, BigDecimal unitPrice, String reference, Long userId) {
        StockMovementRequest request = StockMovementRequest.builder()
                .batchId(batchId)
                .movementType(StockMovement.MovementType.STOCK_IN)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .referenceNumber(reference)
                .build();
        createMovement(request, userId);
    }

    @Override
    @Transactional
    public void createStockOutMovement(Long batchId, Integer quantity, String reason, String reference, Long userId) {
        StockMovementRequest request = StockMovementRequest.builder()
                .batchId(batchId)
                .movementType(StockMovement.MovementType.STOCK_OUT)
                .quantity(quantity)
                .reason(reason)
                .referenceNumber(reference)
                .build();
        createMovement(request, userId);
    }

    @Override
    @Transactional
    public void createAdjustmentMovement(Long batchId, Integer quantityBefore, Integer quantityAfter, String reason, Long userId) {
        StockMovementRequest request = StockMovementRequest.builder()
                .batchId(batchId)
                .movementType(StockMovement.MovementType.STOCK_ADJUSTMENT)
                .quantity(quantityAfter)
                .reason(reason)
                .build();
        createMovement(request, userId);
    }

    @Override
    @Transactional
    public void createExpiredMovement(Long batchId, Integer quantity, Long userId) {
        StockMovementRequest request = StockMovementRequest.builder()
                .batchId(batchId)
                .movementType(StockMovement.MovementType.EXPIRED)
                .quantity(quantity)
                .reason("Product expired")
                .build();
        createMovement(request, userId);
    }
}