// src/main/java/com/smartpharma/service/impl/StockBatchServiceImpl.java

package com.smartpharma.service.impl;

import com.smartpharma.dto.request.StockAdjustmentRequest;
import com.smartpharma.dto.request.StockBatchRequest;
import com.smartpharma.dto.response.StockBatchResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Product;
import com.smartpharma.entity.StockBatch;
import com.smartpharma.entity.User;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.StockBatchRepository;
import com.smartpharma.service.StockBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockBatchServiceImpl implements StockBatchService {

    private final StockBatchRepository stockBatchRepository;
    private final ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;

    // ================================
    // ✅ GET all batches
    // ================================
    @Override
    @Transactional(readOnly = true)
    public List<StockBatchResponse> getAllBatches(Long pharmacyId) {
        log.debug("Fetching all active batches for pharmacy: {}", pharmacyId);
        return stockBatchRepository.findByPharmacyIdAndStatus(pharmacyId, StockBatch.BatchStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ================================
    // ✅ GET single batch
    // ================================
    @Override
    @Transactional(readOnly = true)
    public StockBatchResponse getBatch(Long id, Long pharmacyId) {
        log.debug("Fetching batch {} for pharmacy: {}", id, pharmacyId);

        StockBatch batch = stockBatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + id));

        if (!batch.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied: Batch does not belong to this pharmacy");
        }

        return mapToResponse(batch);
    }

    // ================================
    // ✅ POST create new batch
    // ================================
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

        StockBatch batch = StockBatch.builder()
                .product(product)
                .pharmacy(pharmacy)
                .batchNumber(request.getBatchNumber())
                .quantityCurrent(request.getQuantityInitial())
                .quantityInitial(request.getQuantityInitial())
                .expiryDate(request.getExpiryDate())
                .productionDate(request.getProductionDate())
                .buyPrice(request.getBuyPrice())
                .sellPrice(request.getSellPrice())
                .location(request.getLocation())
                .shelf(request.getShelf())
                .warehouse(request.getWarehouse())
                .notes(request.getNotes())
                .createdBy(User.builder().id(userId).build())
                .status(StockBatch.BatchStatus.ACTIVE)
                .build();

        StockBatch saved = stockBatchRepository.save(batch);
        log.info("Batch created successfully: id={}, batchNumber={}", saved.getId(), saved.getBatchNumber());

        return mapToResponse(saved);
    }

    // ================================
    // ✅ PUT update batch
    // ================================
    @Override
    @Transactional
    public StockBatchResponse updateBatch(Long id, StockBatchRequest request, Long pharmacyId, Long userId) {
        log.info("Updating batch {} for pharmacy: {}, user: {}", id, pharmacyId, userId);

        if (userId == null) {
            throw new RuntimeException("Unauthorized: User ID is required");
        }

        StockBatch batch = stockBatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + id));

        if (!batch.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied: Batch does not belong to this pharmacy");
        }

        // Update fields
        batch.setBatchNumber(request.getBatchNumber());
        batch.setQuantityInitial(request.getQuantityInitial());
        batch.setExpiryDate(request.getExpiryDate());
        batch.setProductionDate(request.getProductionDate());
        batch.setBuyPrice(request.getBuyPrice());
        batch.setSellPrice(request.getSellPrice());
        batch.setLocation(request.getLocation());
        batch.setShelf(request.getShelf());
        batch.setWarehouse(request.getWarehouse());
        batch.setNotes(request.getNotes());
        batch.setUpdatedBy(User.builder().id(userId).build());

        StockBatch updated = stockBatchRepository.save(batch);
        log.info("Batch updated successfully: id={}", updated.getId());

        return mapToResponse(updated);
    }

    // ================================
    // ✅ DELETE batch (soft delete)
    // ================================
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

        // Soft delete: change status instead of hard delete
        batch.setStatus(StockBatch.BatchStatus.DISCARDED);
        batch.setUpdatedBy(User.builder().id(userId).build());
        stockBatchRepository.save(batch);

        log.info("Batch marked as discarded: id={}", id);
    }

    // ================================
    // ✅ GET expiring batches
    // ================================
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

    // ================================
    // ✅ GET expired batches
    // ================================
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

    // ================================
    // ✅ POST adjust stock - FIXED: Added pharmacyId parameter
    // ================================
    @Override
    @Transactional
    public StockBatchResponse adjustStock(Long batchId, StockAdjustmentRequest request, Long pharmacyId, Long userId) {
        log.info("Adjusting stock for batch: {}, pharmacy: {}, user: {}", batchId, pharmacyId, userId);

        if (userId == null) {
            throw new RuntimeException("Unauthorized: User ID is required");
        }

        StockBatch batch = stockBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batchId));

        // ✅ Validate pharmacy ownership
        if (!batch.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied: Batch does not belong to this pharmacy");
        }

        // Adjust quantity
        int newQuantity = batch.getQuantityCurrent() + request.getQuantity();
        if (newQuantity < 0) {
            throw new RuntimeException("Insufficient stock for adjustment");
        }

        batch.setQuantityCurrent(newQuantity);

        // Append reason to notes
        if (request.getReason() != null) {
            String currentNotes = batch.getNotes() != null ? batch.getNotes() : "";
            batch.setNotes(currentNotes + "\n[Adjustment] " + request.getReason());
        }

        batch.setUpdatedBy(User.builder().id(userId).build());

        StockBatch updated = stockBatchRepository.save(batch);
        log.info("Stock adjusted successfully: batch={}, newQuantity={}", batchId, newQuantity);

        return mapToResponse(updated);
    }

    // ================================
    // 🔧 Helper: Entity → DTO Mapping
    // ================================
    private StockBatchResponse mapToResponse(StockBatch batch) {
        return StockBatchResponse.builder()
                .id(batch.getId())
                .productId(batch.getProduct().getId())
                .productName(batch.getProduct().getName())
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
                .notes(batch.getNotes())
                .status(batch.getStatus().name())
                .isExpired(batch.isExpired())
                .isExpiringSoon(batch.isExpiringSoon(30))
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }
}