package com.smartpharma.dto.response;

import com.smartpharma.entity.DemandPrediction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for API response - demand prediction data.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DemandPredictionResponse {

    private Long predictionId;
    private Long productId;
    private String productName;
    private String productCode;
    private Long pharmacyId;
    private LocalDate predictionDate;
    private Integer predictedQuantity;
    private Integer currentStock;
    private Integer recommendedOrder;
    private BigDecimal confidenceLevel;
    private String algorithmVersion;
    private String trend;
    private String seasonalityFactor;
    private String recommendation;
    private LocalDateTime createdAt;

    // Map entity to response DTO
    public static DemandPredictionResponse fromEntity(DemandPrediction prediction, Integer currentStock) {
        if (prediction.getProduct() == null) {
            return DemandPredictionResponse.builder()
                    .predictionId(prediction.getId())
                    .productId(null).productName("Unknown").productCode(null)
                    .pharmacyId(prediction.getPharmacy() != null ? prediction.getPharmacy().getId() : null)
                    .predictionDate(prediction.getPredictionDate())
                    .predictedQuantity(prediction.getPredictedQuantity())
                    .currentStock(currentStock).recommendedOrder(0)
                    .confidenceLevel(prediction.getConfidenceLevel())
                    .algorithmVersion(prediction.getAlgorithmVersion())
                    .trend("stable").seasonalityFactor("medium")
                    .recommendation("Product data unavailable")
                    .createdAt(prediction.getCreatedAt()).build();
        }

        Integer recommendedOrder = Math.max(0,
                prediction.getPredictedQuantity() - (currentStock != null ? currentStock : 0));

        String trend = calculateTrend(prediction.getPredictedQuantity(), currentStock);
        String seasonality = getSeasonalityFactor(prediction.getPredictionDate());
        String productName = prediction.getProduct().getName();

        String recommendation = generateRecommendation(
                productName, prediction.getPredictedQuantity(),
                currentStock, recommendedOrder, trend);

        return DemandPredictionResponse.builder()
                .predictionId(prediction.getId())
                .productId(prediction.getProduct().getId())
                .productName(productName)
                .productCode(prediction.getProduct().getCode())
                .pharmacyId(prediction.getPharmacy() != null ? prediction.getPharmacy().getId() : null)
                .predictionDate(prediction.getPredictionDate())
                .predictedQuantity(prediction.getPredictedQuantity())
                .currentStock(currentStock)
                .recommendedOrder(recommendedOrder)
                .confidenceLevel(prediction.getConfidenceLevel())
                .algorithmVersion(prediction.getAlgorithmVersion())
                .trend(trend).seasonalityFactor(seasonality)
                .recommendation(recommendation)
                .createdAt(prediction.getCreatedAt()).build();
    }

    // Determine demand trend based on predicted vs current stock
    private static String calculateTrend(Integer predicted, Integer current) {
        if (current == null) return "stable";
        if (predicted > current * 1.2) return "increasing";
        if (predicted < current * 0.8) return "decreasing";
        return "stable";
    }

    // Get seasonality factor based on month
    private static String getSeasonalityFactor(LocalDate date) {
        if (date == null) return "medium";
        int month = date.getMonthValue();
        if (month == 12 || month == 1 || month == 2) return "high";
        if (month >= 3 && month <= 5) return "medium";
        if (month >= 6 && month <= 8) return "low";
        return "medium";
    }

    // Generate Arabic recommendation message
    private static String generateRecommendation(
            String productName, Integer predicted, Integer current,
            Integer recommended, String trend) {

        if (productName == null) productName = "المنتج";

        if (recommended <= 0) {
            return String.format("المخزون الحالي كافٍ لـ '%s' للأسبوع القادم", productName);
        }
        if ("increasing".equals(trend)) {
            return String.format("الطلب على '%s' في ارتفاع - ننصح بطلب %d وحدة إضافي",
                    productName, recommended);
        }
        return String.format("ننصح بطلب %d وحدة من '%s' للأسبوع القادم", recommended, productName);
    }
}