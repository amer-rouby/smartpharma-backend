package com.smartpharma.service.impl;

import com.smartpharma.dto.request.StockBatchRequest;
import com.smartpharma.dto.response.StockBatchResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Product;
import com.smartpharma.entity.StockBatch;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.StockBatchRepository;
import com.smartpharma.service.StockBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockBatchServiceImpl implements StockBatchService {

    private final StockBatchRepository stockBatchRepository;
    private final ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StockBatchResponse> getAllBatches(Long pharmacyId) {
        return stockBatchRepository.findByPharmacyIdAndStatus(pharmacyId, StockBatch.BatchStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StockBatchResponse getBatch(Long id, Long pharmacyId) {
        StockBatch batch = stockBatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        if (!batch.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        return mapToResponse(batch);
    }

    @Override
    @Transactional
    public StockBatchResponse createBatch(StockBatchRequest request, Long pharmacyId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

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
                .buyPrice(request.getBuyPrice())
                .sellPrice(request.getSellPrice())
                .location(request.getLocation())
                .status(StockBatch.BatchStatus.ACTIVE)
                .build();

        stockBatchRepository.save(batch);
        return mapToResponse(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBatchResponse> getExpiringBatches(Long pharmacyId, int days) {
        LocalDate thresholdDate = LocalDate.now().plusDays(days);
        return stockBatchRepository.findExpiringBatches(pharmacyId, thresholdDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 🔧 Helper Method: Entity → DTO Mapping
    // ==========================================

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
                .buyPrice(batch.getBuyPrice())
                .sellPrice(batch.getSellPrice())
                .location(batch.getLocation())
                .status(batch.getStatus().name())
                .isExpired(batch.isExpired())
                .isExpiringSoon(batch.isExpiringSoon(30))
                .createdAt(batch.getCreatedAt())
                .build();
    }
}