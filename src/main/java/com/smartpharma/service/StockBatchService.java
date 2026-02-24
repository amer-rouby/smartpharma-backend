// src/main/java/com/smartpharma/service/StockBatchService.java

package com.smartpharma.service;

import com.smartpharma.dto.request.StockAdjustmentRequest;
import com.smartpharma.dto.request.StockBatchRequest;
import com.smartpharma.dto.response.StockBatchResponse;

import java.util.List;

public interface StockBatchService {

    List<StockBatchResponse> getAllBatches(Long pharmacyId);
    StockBatchResponse getBatch(Long id, Long pharmacyId);

    // ✅ FIXED: جميع الـ methods فيها pharmacyId + userId
    StockBatchResponse createBatch(StockBatchRequest request, Long pharmacyId, Long userId);
    StockBatchResponse updateBatch(Long id, StockBatchRequest request, Long pharmacyId, Long userId);
    void deleteBatch(Long id, Long pharmacyId, Long userId);

    List<StockBatchResponse> getExpiringBatches(Long pharmacyId, int days);
    List<StockBatchResponse> getExpiredBatches(Long pharmacyId);

    // ✅ FIXED: adjustStock فيها pharmacyId + userId
    StockBatchResponse adjustStock(Long batchId, StockAdjustmentRequest request, Long pharmacyId, Long userId);
}