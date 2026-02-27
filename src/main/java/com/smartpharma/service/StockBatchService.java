package com.smartpharma.service;

import com.smartpharma.dto.request.StockAdjustmentRequest;
import com.smartpharma.dto.request.StockBatchRequest;
import com.smartpharma.dto.response.StockBatchResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface StockBatchService {

    Page<StockBatchResponse> getAllBatches(Long pharmacyId, int page, int size);
    StockBatchResponse getBatch(Long id, Long pharmacyId);

    StockBatchResponse createBatch(StockBatchRequest request, Long pharmacyId, Long userId);
    StockBatchResponse updateBatch(Long id, StockBatchRequest request, Long pharmacyId, Long userId);
    void deleteBatch(Long id, Long pharmacyId, Long userId);

    List<StockBatchResponse> getExpiringBatches(Long pharmacyId, int days);
    List<StockBatchResponse> getExpiredBatches(Long pharmacyId);

    StockBatchResponse adjustStock(Long batchId, StockAdjustmentRequest request, Long userId);
}