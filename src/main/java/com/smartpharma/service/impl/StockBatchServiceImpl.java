package com.smartpharma.service.impl;

import com.smartpharma.dto.request.StockAdjustmentRequest;
import com.smartpharma.dto.request.StockBatchRequest;
import com.smartpharma.dto.response.StockAdjustmentHistoryDTO;
import com.smartpharma.dto.response.StockBatchResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Product;
import com.smartpharma.entity.StockAdjustmentHistory;
import com.smartpharma.entity.StockBatch;
import com.smartpharma.entity.User;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.StockAdjustmentHistoryRepository;
import com.smartpharma.repository.StockBatchRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.service.StockBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockBatchServiceImpl implements StockBatchService {

    private final StockBatchRepository stockBatchRepository;
    private final ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;
    private final StockAdjustmentHistoryRepository stockAdjustmentHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<StockBatchResponse> getAllBatches(Long pharmacyId, int page, int size) {
        log.debug("Fetching batches for pharmacy: {}, page: {}, size: {}", pharmacyId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return stockBatchRepository.findByPharmacyIdAndStatus(pharmacyId, StockBatch.BatchStatus.ACTIVE, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public StockBatchResponse getBatch(Long id, Long pharmacyId) {
        log.debug("Fetching batch {} for pharmacy: {}", id, pharmacyId);
        StockBatch batch = stockBatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + id));

        if (!Hibernate.isInitialized(batch.getPharmacy())) {
            Hibernate.initialize(batch.getPharmacy());
        }
        if (!batch.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied: Batch does not belong to this pharmacy");
        }
        return mapToResponse(batch);
    }

    @Override
    @Transactional
    public StockBatchResponse createBatch(StockBatchRequest request, Long pharmacyId, Long userId) {
        log.info("Creating new batch for pharmacy: {}, user: {}, product: {}",
                pharmacyId, userId, request.getProductId());

        if (userId == null) {
            throw new RuntimeException("Unauthorized: User ID is required");
        }

        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found with id: " + pharmacyId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.getProductId()));

        if (!product.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Product does not belong to this pharmacy");
        }

        BigDecimal buyPrice = request.getBuyPrice();
        if (buyPrice == null) {
            buyPrice = product.getBuyPrice();
            if (buyPrice == null) {
                buyPrice = BigDecimal.ZERO;
            }
        }

        BigDecimal sellPrice = request.getSellPrice();
        if (sellPrice == null) {
            sellPrice = product.getSellPrice();
            if (sellPrice == null) {
                sellPrice = buyPrice.multiply(BigDecimal.valueOf(1.25)).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
        }

        User userRef = null;
        if (userId != null) {
            try {
                userRef = userRepository.getReferenceById(userId);
            } catch (Exception e) {
                log.warn("Could not load user reference for userId: {}", userId);
            }
        }

        StockBatch batch = StockBatch.builder()
                .product(product)
                .pharmacy(pharmacy)
                .batchNumber(request.getBatchNumber())
                .quantityCurrent(request.getQuantityInitial())
                .quantityInitial(request.getQuantityInitial())
                .expiryDate(request.getExpiryDate())
                .productionDate(request.getProductionDate())
                .buyPrice(buyPrice)
                .sellPrice(sellPrice)
                .location(request.getLocation())
                .shelf(request.getShelf())
                .warehouse(request.getWarehouse())
                .notes(request.getNotes())
                .createdBy(userRef)
                .status(StockBatch.BatchStatus.ACTIVE)
                .build();

        StockBatch saved = stockBatchRepository.save(batch);
        log.info("Batch created successfully: id={}, batchNumber={}", saved.getId(), saved.getBatchNumber());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public StockBatchResponse updateBatch(Long id, StockBatchRequest request, Long pharmacyId, Long userId) {
        log.info("Updating batch {} for pharmacy: {}, user: {}", id, pharmacyId, userId);

        if (userId == null) {
            log.warn("User ID is null for batch update: {}. Proceeding without user tracking.", id);
        }

        StockBatch batch = stockBatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + id));

        if (!batch.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied: Batch does not belong to this pharmacy");
        }

        batch.setBatchNumber(request.getBatchNumber());
        batch.setQuantityInitial(request.getQuantityInitial());
        batch.setQuantityCurrent(request.getQuantityCurrent());
        batch.setExpiryDate(request.getExpiryDate());
        batch.setProductionDate(request.getProductionDate());

        if (request.getStatus() != null) {
            batch.setStatus(StockBatch.BatchStatus.valueOf(request.getStatus()));
        }

        if (request.getBuyPrice() != null) {
            batch.setBuyPrice(request.getBuyPrice());
        }
        if (request.getSellPrice() != null) {
            batch.setSellPrice(request.getSellPrice());
        }

        batch.setLocation(request.getLocation());
        batch.setShelf(request.getShelf());
        batch.setWarehouse(request.getWarehouse());
        batch.setNotes(request.getNotes());
        batch.setUpdatedAt(java.time.LocalDateTime.now());

        StockBatch updated = stockBatchRepository.save(batch);
        log.info("Batch updated successfully: id={}, status={}", updated.getId(), updated.getStatus());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBatch(Long id, Long pharmacyId, Long userId) {
        log.info("Deleting batch {} for pharmacy: {}, user: {}", id, pharmacyId, userId);

        if (userId == null) {
            throw new RuntimeException("Unauthorized: User ID is required");
        }

        StockBatch batch = stockBatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + id));

        if (!batch.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied: Batch does not belong to this pharmacy");
        }

        batch.setStatus(StockBatch.BatchStatus.DISCARDED);
        batch.setUpdatedAt(LocalDateTime.now());
        stockBatchRepository.save(batch);
        log.info("Batch marked as discarded: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBatchResponse> getExpiringBatches(Long pharmacyId, int days) {
        log.debug("Fetching expiring batches for pharmacy: {}, days: {}", pharmacyId, days);
        LocalDate thresholdDate = LocalDate.now().plusDays(days);
        return stockBatchRepository.findExpiringBatches(pharmacyId, thresholdDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBatchResponse> getExpiredBatches(Long pharmacyId) {
        log.debug("Fetching expired batches for pharmacy: {}", pharmacyId);
        LocalDate today = LocalDate.now();
        return stockBatchRepository.findExpiredBatches(pharmacyId, today)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StockBatchResponse adjustStock(Long batchId, StockAdjustmentRequest request, Long userId) {
        log.info("Adjusting stock for batch: {}, type: {}, quantity: {}, user: {}",
                batchId, request.getType(), request.getQuantity(), userId);

        if (userId == null) {
            throw new RuntimeException("Unauthorized: User ID is required for stock adjustment");
        }

        StockBatch batch = stockBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batchId));

        Integer currentQuantity = batch.getQuantityCurrent();
        Integer adjustmentQuantity = request.getQuantity();
        String type = request.getType();

        Integer newQuantity = switch (type) {
            case "ADD" -> currentQuantity + adjustmentQuantity;
            case "REMOVE" -> {
                if (adjustmentQuantity > currentQuantity) {
                    throw new RuntimeException("Insufficient stock: current=" + currentQuantity + ", requested=" + adjustmentQuantity);
                }
                yield currentQuantity - adjustmentQuantity;
            }
            case "CORRECTION" -> adjustmentQuantity;
            default -> throw new IllegalArgumentException("Invalid adjustment type: " + type);
        };

        batch.setQuantityCurrent(newQuantity);
        updateBatchStatus(batch, newQuantity);

        StockAdjustmentHistory history = StockAdjustmentHistory.builder()
                .batch(batch)
                .type(type)
                .quantity(adjustmentQuantity)
                .reason(request.getReason())
                .previousQuantity(currentQuantity)
                .newQuantity(newQuantity)
                .notes(request.getNotes())
                .adjustedBy(userId)
                .build();

        stockAdjustmentHistoryRepository.save(history);

        String shortNote = String.format("[%s] %s: %d→%d",
                request.getReason(), type, currentQuantity, newQuantity);
        batch.setNotes(shortNote);

        batch.setUpdatedAt(LocalDateTime.now());

        StockBatch updated = stockBatchRepository.save(batch);
        log.info("Stock adjusted | batchId: {} | type: {} | qty: {} | {}→{} | status: {}",
                batchId, type, adjustmentQuantity, currentQuantity, newQuantity, updated.getStatus());

        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentHistoryDTO> getAdjustmentHistory(Long batchId, Long pharmacyId) {
        StockBatch batch = stockBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        if (!batch.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        return stockAdjustmentHistoryRepository.findByBatchIdOrderByAdjustmentDateDesc(batchId)
                .stream()
                .map(this::mapHistoryToDTO)
                .collect(Collectors.toList());
    }

    private void updateBatchStatus(StockBatch batch, Integer newQuantity) {
        if (batch.isExpired()) {
            batch.setStatus(StockBatch.BatchStatus.EXPIRED);
        } else if (newQuantity <= 0) {
            batch.setStatus(StockBatch.BatchStatus.EXPIRED);
        } else if (newQuantity < batch.getProduct().getMinStockLevel()) {
            batch.setStatus(StockBatch.BatchStatus.LOW);
        } else {
            batch.setStatus(StockBatch.BatchStatus.ACTIVE);
        }
    }

    private StockBatchResponse mapToResponse(StockBatch batch) {
        try {
            if (batch == null) {
                return null;
            }

            String productName = "منتج غير متاح";
            String productBarcode = null;
            Long productId = null;

            try {
                if (batch.getProduct() != null) {
                    if (!Hibernate.isInitialized(batch.getProduct())) {
                        try {
                            Hibernate.initialize(batch.getProduct());
                        } catch (Exception e) {
                            log.warn("Could not initialize product for batch {}: {}",
                                    batch.getId(), e.getMessage());
                        }
                    }

                    if (batch.getProduct().getDeletedAt() == null) {
                        productId = batch.getProduct().getId();
                        productName = batch.getProduct().getName() != null ?
                                batch.getProduct().getName() : "منتج غير متاح";
                        productBarcode = batch.getProduct().getBarcode();
                    }
                }
            } catch (Exception e) {
                log.warn("Error loading product for batch {}: {}", batch.getId(), e.getMessage());
            }

            return StockBatchResponse.builder()
                    .id(batch.getId())
                    .productId(productId)
                    .productName(productName)
                    .productBarcode(productBarcode)
                    .pharmacyId(batch.getPharmacy().getId())
                    .batchNumber(batch.getBatchNumber())
                    .quantityCurrent(batch.getQuantityCurrent())
                    .quantityInitial(batch.getQuantityInitial())
                    .expiryDate(batch.getExpiryDate())
                    .productionDate(batch.getProductionDate())
                    .buyPrice(batch.getBuyPrice())
                    .sellPrice(batch.getSellPrice())
                    .location(batch.getLocation())
                    .shelf(batch.getShelf())
                    .warehouse(batch.getWarehouse())
                    .status(batch.getStatus() != null ? batch.getStatus().name() : null)
                    .notes(batch.getNotes())
                    .createdAt(batch.getCreatedAt())
                    .updatedAt(batch.getUpdatedAt())
                    .isExpired(batch.isExpired())
                    .isExpiringSoon(batch.isExpiringSoon(30))
                    .build();

        } catch (Exception e) {
            log.error("Failed to map batch {} to response: {}",
                    batch != null ? batch.getId() : "unknown", e.getMessage());
            return StockBatchResponse.builder()
                    .id(batch != null ? batch.getId() : null)
                    .productName("خطأ في تحميل البيانات")
                    .build();
        }
    }

    private StockAdjustmentHistoryDTO mapHistoryToDTO(StockAdjustmentHistory history) {
        try {
            String productName = "منتج غير متاح";
            try {
                if (history.getBatch() != null && history.getBatch().getProduct() != null) {
                    if (!Hibernate.isInitialized(history.getBatch().getProduct())) {
                        Hibernate.initialize(history.getBatch().getProduct());
                    }
                    if (history.getBatch().getProduct().getDeletedAt() == null) {
                        productName = history.getBatch().getProduct().getName() != null ?
                                history.getBatch().getProduct().getName() : "منتج غير متاح";
                    }
                }
            } catch (Exception e) {
                log.warn("Could not load product for history {}: {}", history.getId(), e.getMessage());
            }

            return StockAdjustmentHistoryDTO.builder()
                    .id(history.getId())
                    .batchId(history.getBatch() != null ? history.getBatch().getId() : null)
                    .batchNumber(history.getBatch() != null ? history.getBatch().getBatchNumber() : null)
                    .productName(productName)
                    .type(history.getType())
                    .quantity(history.getQuantity())
                    .reason(history.getReason())
                    .previousQuantity(history.getPreviousQuantity())
                    .newQuantity(history.getNewQuantity())
                    .notes(history.getNotes())
                    .adjustmentDate(history.getAdjustmentDate())
                    .adjustedBy(history.getAdjustedBy())
                    .adjustedByName(history.getAdjustedByName())
                    .build();
        } catch (Exception e) {
            log.error("Failed to map history {} to DTO", history != null ? history.getId() : "unknown", e);
            return StockAdjustmentHistoryDTO.builder()
                    .id(history != null ? history.getId() : null)
                    .productName("خطأ في تحميل البيانات")
                    .build();
        }
    }
}