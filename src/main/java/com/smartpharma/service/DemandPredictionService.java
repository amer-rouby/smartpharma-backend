package com.smartpharma.service;

import com.smartpharma.dto.request.UpdatePredictionDTO;
import com.smartpharma.dto.response.DemandPredictionResponse;
import com.smartpharma.dto.response.PurchaseOrderSummaryDTO;
import com.smartpharma.dto.response.ReorderRecommendationDTO;
import com.smartpharma.dto.response.ShareLinkResponse;
import com.smartpharma.dto.response.SupplierReorderGroupDTO;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DemandPredictionService {

    void generatePredictions(Long pharmacyId, LocalDate forDate);

    void generateWeeklyPredictionsForAllPharmacies();

    void updatePastPredictionsWithActuals();

    DemandPredictionResponse generatePredictionForProduct(Long productId, Long pharmacyId, LocalDate forDate);

    List<DemandPredictionResponse> getUpcomingPredictions(Long pharmacyId, int daysAhead);

    Page<DemandPredictionResponse> getPredictions(Long pharmacyId, int page, int size);

    DemandPredictionResponse getPredictionById(Long predictionId, Long pharmacyId);

    void updatePredictionWithActual(Long predictionId, Integer actualQuantity, Long pharmacyId);

    Map<String, Object> getAccuracyStats(Long pharmacyId);

    Integer calculateSimpleForecast(List<Integer> historicalSales, LocalDate predictionDate, String productCategory);

    DemandPredictionResponse updatePrediction(Long predictionId, UpdatePredictionDTO updates, Long pharmacyId);

    void deletePrediction(Long predictionId, Long pharmacyId);

    byte[] exportPredictionToPdf(Long predictionId, Long pharmacyId);

    byte[] exportPredictionToExcel(Long predictionId, Long pharmacyId);

    ShareLinkResponse generateShareLink(Long predictionId, Long pharmacyId, Long userId, int expiryHours);

    PurchaseOrderSummaryDTO createPurchaseFromPrediction(Long predictionId, Long pharmacyId, Long userId);

    List<ReorderRecommendationDTO> getReorderRecommendations(Long pharmacyId);

    List<SupplierReorderGroupDTO> getReorderRecommendationsBySupplier(Long pharmacyId);
}