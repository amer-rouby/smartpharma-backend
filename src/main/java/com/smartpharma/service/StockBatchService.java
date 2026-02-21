package com.smartpharma.service;

import com.smartpharma.dto.request.StockBatchRequest;
import com.smartpharma.dto.response.StockBatchResponse;

import java.util.List;

public interface StockBatchService {

    List<StockBatchResponse> getAllBatches(Long pharmacyId);

    StockBatchResponse getBatch(Long id, Long pharmacyId);

    StockBatchResponse createBatch(StockBatchRequest request, Long pharmacyId);

    List<StockBatchResponse> getExpiringBatches(Long pharmacyId, int days);
}