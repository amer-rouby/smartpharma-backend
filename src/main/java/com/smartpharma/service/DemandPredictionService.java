package com.smartpharma.service;

import com.smartpharma.dto.request.UpdatePredictionDTO;
import com.smartpharma.dto.response.DemandPredictionResponse;
import com.smartpharma.dto.response.PurchaseOrderSummaryDTO;
import com.smartpharma.dto.response.ShareLinkDTO;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DemandPredictionService {

    void generatePredictions(Long pharmacyId, LocalDate forDate);

    DemandPredictionResponse generatePredictionForProduct(Long productId, Long pharmacyId, LocalDate forDate);

    List<DemandPredictionResponse> getUpcomingPredictions(Long pharmacyId, int daysAhead);

    Page<DemandPredictionResponse> getPredictions(Long pharmacyId, int page, int size);

    void updatePredictionWithActual(Long predictionId, Integer actualQuantity);

    Map<String, Object> getAccuracyStats(Long pharmacyId);

    Integer calculateSimpleForecast(List<Integer> historicalSales, LocalDate predictionDate, String productCategory);

    DemandPredictionResponse updatePrediction(Long predictionId, UpdatePredictionDTO updates, Long pharmacyId);

    void deletePrediction(Long predictionId, Long pharmacyId);

    byte[] exportPredictionToPdf(Long predictionId, Long pharmacyId);

    byte[] exportPredictionToExcel(Long predictionId, Long pharmacyId);

    ShareLinkDTO generateShareLink(Long predictionId, Long pharmacyId, int expiryHours);

    PurchaseOrderSummaryDTO createPurchaseFromPrediction(Long predictionId, Long pharmacyId, Long userId);
}