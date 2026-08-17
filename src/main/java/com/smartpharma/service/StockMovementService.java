package com.smartpharma.service;

import com.smartpharma.dto.request.StockMovementRequest;
import com.smartpharma.dto.response.StockMovementResponse;
import com.smartpharma.dto.response.StockMovementStats;
import com.smartpharma.entity.StockMovement;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface StockMovementService {

    StockMovementResponse createMovement(StockMovementRequest request, Long userId, Long pharmacyId);

    Page<StockMovementResponse> getMovementsByPharmacy(Long pharmacyId, int page, int size);
    Page<StockMovementResponse> getMovementsByBatch(Long batchId, Long pharmacyId, int page, int size);
    Page<StockMovementResponse> getMovementsByDateRange(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate, StockMovement.MovementType type, int page, int size);

    StockMovementStats getMovementStats(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate);

    void createStockInMovement(Long batchId, Integer quantity, BigDecimal unitPrice, String reference, Long userId);
    void createStockOutMovement(Long batchId, Integer quantity, String reason, String reference, Long userId);
    void createAdjustmentMovement(Long batchId, Integer quantityBefore, Integer quantityAfter, String reason, Long userId);
    void createExpiredMovement(Long batchId, Integer quantity, Long userId);
}