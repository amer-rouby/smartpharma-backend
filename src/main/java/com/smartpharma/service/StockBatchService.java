// src/main/java/com/smartpharma/service/StockBatchService.java

package com.smartpharma.service;

import com.smartpharma.dto.request.StockAdjustmentRequest;
import com.smartpharma.dto.request.StockBatchRequest;
import com.smartpharma.dto.response.StockBatchResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface StockBatchService {

    // ✅ Pagination support
    Page<StockBatchResponse> getAllBatches(Long pharmacyId, int page, int size);
    StockBatchResponse getBatch(Long id, Long pharmacyId);

    // ✅ CRUD operations
    StockBatchResponse createBatch(StockBatchRequest request, Long pharmacyId, Long userId);
    StockBatchResponse updateBatch(Long id, StockBatchRequest request, Long pharmacyId, Long userId);
    void deleteBatch(Long id, Long pharmacyId, Long userId);

    // ✅ Reports
    List<StockBatchResponse> getExpiringBatches(Long pharmacyId, int days);
    List<StockBatchResponse> getExpiredBatches(Long pharmacyId);

    // ✅ Stock adjustment
    StockBatchResponse adjustStock(Long batchId, StockAdjustmentRequest request, Long userId);
}