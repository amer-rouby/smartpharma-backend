package com.smartpharma.service;

import com.smartpharma.dto.response.DemandPredictionResponse;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service interface for demand prediction operations.
 */
public interface DemandPredictionService {

    // Generate predictions for all products in pharmacy
    void generatePredictions(Long pharmacyId, LocalDate forDate);

    // Generate prediction for single product
    DemandPredictionResponse generatePredictionForProduct(
            Long productId, Long pharmacyId, LocalDate forDate);

    // Get upcoming predictions for next N days
    List<DemandPredictionResponse> getUpcomingPredictions(Long pharmacyId, int daysAhead);

    // Get paginated predictions list
    Page<DemandPredictionResponse> getPredictions(Long pharmacyId, int page, int size);

    // Update prediction with actual sales data
    void updatePredictionWithActual(Long predictionId, Integer actualQuantity);

    // Get accuracy statistics for pharmacy
    Map<String, Object> getAccuracyStats(Long pharmacyId);

    // Simple forecasting algorithm (MVP)
    Integer calculateSimpleForecast(
            List<Integer> historicalSales,
            LocalDate predictionDate,
            String productCategory);
}