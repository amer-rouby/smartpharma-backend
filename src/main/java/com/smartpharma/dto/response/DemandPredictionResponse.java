package com.smartpharma.dto.response;

import com.smartpharma.entity.DemandPrediction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private Integer daysUntilStockout;
    private String riskLevel;
    private LocalDateTime createdAt;

    public static DemandPredictionResponse fromEntity(DemandPrediction prediction, Integer currentStock) {
        return fromEntity(prediction, currentStock, true);
    }

    public static DemandPredictionResponse fromEntity(DemandPrediction prediction, Integer currentStock, boolean includeRiskData) {
        Integer predictedQty = prediction.getPredictedQuantity() != null ? prediction.getPredictedQuantity() : 0;
        Integer stock = currentStock != null ? currentStock : 0;
        Integer recommendedOrder = Math.max(0, predictedQty - stock);

        String trend = calculateTrend(predictedQty, stock);
        String seasonality = getSeasonalityFactor(prediction.getPredictionDate());
        String productName = prediction.getProduct() != null && prediction.getProduct().getName() != null
                ? prediction.getProduct().getName() : "Unknown";
        String recommendation = generateRecommendation(productName, predictedQty, stock, recommendedOrder, trend);

        Integer daysUntilStockout = null;
        String riskLevel = null;
        if (includeRiskData) {
            daysUntilStockout = calculateDaysUntilStockout(stock, predictedQty);
            riskLevel = calculateRiskLevel(stock, daysUntilStockout);
        }

        return DemandPredictionResponse.builder()
                .predictionId(prediction.getId())
                .productId(prediction.getProduct() != null ? prediction.getProduct().getId() : null)
                .productName(productName)
                .productCode(prediction.getProduct() != null ? prediction.getProduct().getCode() : null)
                .pharmacyId(prediction.getPharmacy() != null ? prediction.getPharmacy().getId() : null)
                .predictionDate(prediction.getPredictionDate())
                .predictedQuantity(predictedQty)
                .currentStock(stock)
                .recommendedOrder(recommendedOrder)
                .confidenceLevel(prediction.getConfidenceLevel())
                .algorithmVersion(prediction.getAlgorithmVersion())
                .trend(trend)
                .seasonalityFactor(seasonality)
                .recommendation(recommendation)
                .daysUntilStockout(daysUntilStockout)
                .riskLevel(riskLevel)
                .createdAt(prediction.getCreatedAt())
                .build();
    }

    // currentStock / average daily demand. Predictions carry a per-day forecast
    // quantity already (see DemandPredictionServiceImpl.calculateSimpleForecast),
    // so it doubles as the daily consumption rate here.
    private static Integer calculateDaysUntilStockout(Integer currentStock, Integer predictedDailyQuantity) {
        if (currentStock == null || currentStock <= 0) return 0;
        if (predictedDailyQuantity == null || predictedDailyQuantity <= 0) return null;
        return currentStock / predictedDailyQuantity;
    }

    private static String calculateRiskLevel(Integer currentStock, Integer daysUntilStockout) {
        if (currentStock != null && currentStock <= 0) return "CRITICAL";
        if (daysUntilStockout == null) return "LOW";
        if (daysUntilStockout < 3) return "CRITICAL";
        if (daysUntilStockout < 7) return "HIGH";
        if (daysUntilStockout < 14) return "MEDIUM";
        return "LOW";
    }

    private static String calculateTrend(Integer predicted, Integer current) {
        if (current == null || current == 0) return "stable";
        if (predicted > current * 1.2) return "increasing";
        if (predicted < current * 0.8) return "decreasing";
        return "stable";
    }

    private static String getSeasonalityFactor(LocalDate date) {
        if (date == null) return "medium";
        int month = date.getMonthValue();
        if (month == 12 || month == 1 || month == 2) return "high";
        if (month >= 3 && month <= 5) return "medium";
        if (month >= 6 && month <= 8) return "low";
        return "medium";
    }

    private static String generateRecommendation(String productName, Integer predicted, Integer current,
                                                 Integer recommended, String trend) {
        if (recommended <= 0) {
            return String.format("Current stock is sufficient for '%s' for the next week", productName);
        }
        if ("increasing".equals(trend)) {
            return String.format("Demand for '%s' is increasing - recommended to order %d additional unit(s)", productName, recommended);
        }
        return String.format("Recommended to order %d unit(s) of '%s' for the next week", recommended, productName);
    }
}