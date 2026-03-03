package com.smartpharma.service.impl;

import com.smartpharma.dto.request.UpdatePredictionDTO;
import com.smartpharma.dto.response.DemandPredictionResponse;
import com.smartpharma.dto.response.PurchaseOrderSummaryDTO;
import com.smartpharma.dto.response.ShareLinkDTO;
import com.smartpharma.entity.DemandPrediction;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Product;
import com.smartpharma.entity.SaleItem;
import com.smartpharma.repository.DemandPredictionRepository;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.SaleItemRepository;
import com.smartpharma.service.DemandPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemandPredictionServiceImpl implements DemandPredictionService {

    private final DemandPredictionRepository predictionRepository;
    private final ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;
    private final SaleItemRepository saleItemRepository;

    private static final int MOVING_AVG_DAYS = 14;
    private static final int DEFAULT_PREDICTION = 10;
    private static final BigDecimal CONFIDENCE_BASE = BigDecimal.valueOf(0.75);

    @Override
    @Transactional
    public void generatePredictions(Long pharmacyId, LocalDate forDate) {
        log.info("Starting prediction generation for pharmacy: {}, date: {}", pharmacyId, forDate);
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found with ID: " + pharmacyId));
        List<Product> products = productRepository.findByPharmacyId(pharmacyId);
        int success = 0;
        for (Product product : products) {
            try {
                generatePredictionForProduct(product.getId(), pharmacyId, forDate);
                success++;
            } catch (Exception e) {
                log.error("Failed for product ID {}: {}", product.getId(), e.getMessage(), e);
            }
        }
        log.info("Completed. Success: {}, Failed: {}", success, products.size() - success);
    }

    @Override
    @Transactional
    public DemandPredictionResponse generatePredictionForProduct(Long productId, Long pharmacyId, LocalDate forDate) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found: " + pharmacyId));
        List<Integer> historicalSales = getHistoricalSales(productId, pharmacyId, 30);
        Integer predictedQuantity = calculateSimpleForecast(historicalSales, forDate, product.getCategory());
        BigDecimal confidence = calculateConfidence(historicalSales, predictedQuantity);
        Integer currentStock = getCurrentStock(productId);
        DemandPrediction prediction = DemandPrediction.builder()
                .product(product)
                .pharmacy(pharmacy)
                .predictionDate(forDate)
                .predictedQuantity(predictedQuantity)
                .confidenceLevel(confidence)
                .algorithmVersion("v1-simple-moving-average")
                .factorsApplied(buildFactorsJson(forDate, product.getCategory()))
                .build();
        Optional<DemandPrediction> existing = predictionRepository
                .findByProductIdAndPharmacyIdAndPredictionDate(productId, pharmacyId, forDate);
        if (existing.isPresent()) {
            prediction.setId(existing.get().getId());
        }
        DemandPrediction saved = predictionRepository.save(prediction);
        log.info("Prediction saved: product={}, date={}, predicted={}", productId, forDate, predictedQuantity);
        return DemandPredictionResponse.fromEntity(saved, currentStock);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandPredictionResponse> getUpcomingPredictions(Long pharmacyId, int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead);
        List<DemandPrediction> predictions = predictionRepository
                .findUpcomingPredictions(pharmacyId, today, endDate);
        log.info("Found {} upcoming predictions for pharmacy {}", predictions.size(), pharmacyId);
        return predictions.stream()
                .map(p -> {
                    Long prodId = p.getProduct() != null ? p.getProduct().getId() : null;
                    return DemandPredictionResponse.fromEntity(p, getCurrentStock(prodId));
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DemandPredictionResponse> getPredictions(Long pharmacyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return predictionRepository.findByPharmacyId(pharmacyId, pageable)
                .map(p -> {
                    Long prodId = p.getProduct() != null ? p.getProduct().getId() : null;
                    return DemandPredictionResponse.fromEntity(p, getCurrentStock(prodId));
                });
    }

    @Override
    @Transactional
    public void updatePredictionWithActual(Long predictionId, Integer actualQuantity) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found: " + predictionId));
        prediction.setActualQuantity(actualQuantity);
        if (prediction.getPredictedQuantity() != null && prediction.getPredictedQuantity() > 0) {
            int error = Math.abs(actualQuantity - prediction.getPredictedQuantity());
            BigDecimal accuracy = BigDecimal.valueOf(Math.max(0, 100.0 - (error * 100.0 / prediction.getPredictedQuantity())));
            prediction.setAccuracyPercentage(accuracy);
        }
        predictionRepository.save(prediction);
        log.info("Prediction {} updated with actual: {}, accuracy: {}%",
                predictionId, actualQuantity, prediction.getAccuracyPercentage());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAccuracyStats(Long pharmacyId) {
        Long totalPredictions = predictionRepository.countByPharmacyId(pharmacyId);
        BigDecimal averageAccuracy = predictionRepository.calculateAverageAccuracyByPharmacy(pharmacyId);
        String lastUpdated = predictionRepository.findLatestPredictionDateByPharmacy(pharmacyId)
                .map(LocalDate::toString)
                .orElse(LocalDate.now().toString());
        return Map.of(
                "totalPredictions", totalPredictions != null ? totalPredictions : 0,
                "averageAccuracy", averageAccuracy != null ? averageAccuracy.doubleValue() : 0.0,
                "lastUpdated", lastUpdated
        );
    }

    @Override
    public Integer calculateSimpleForecast(List<Integer> historicalSales, LocalDate predictionDate, String productCategory) {
        if (historicalSales == null || historicalSales.isEmpty()) {
            return DEFAULT_PREDICTION;
        }
        int movingAvg = calculateMovingAverage(historicalSales, MOVING_AVG_DAYS);
        BigDecimal seasonalityFactor = getSeasonalityMultiplier(predictionDate, productCategory);
        BigDecimal adjusted = BigDecimal.valueOf(movingAvg).multiply(seasonalityFactor);
        BigDecimal trendFactor = calculateTrendFactor(historicalSales);
        adjusted = adjusted.multiply(trendFactor);
        if (predictionDate != null &&
                (predictionDate.getDayOfWeek() == DayOfWeek.FRIDAY || predictionDate.getDayOfWeek() == DayOfWeek.SATURDAY)) {
            adjusted = adjusted.multiply(BigDecimal.valueOf(1.15));
        }
        return Math.max(1, adjusted.intValue());
    }

    @Override
    @Transactional
    public DemandPredictionResponse updatePrediction(Long predictionId, UpdatePredictionDTO updates, Long pharmacyId) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found: " + predictionId));
        if (!prediction.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied: Prediction does not belong to this pharmacy");
        }
        if (updates.getPredictedQuantity() != null && updates.getPredictedQuantity() > 0) {
            prediction.setPredictedQuantity(updates.getPredictedQuantity());
        }
        if (updates.getConfidenceLevel() != null) {
            BigDecimal confidence = updates.getConfidenceLevel();
            if (confidence.compareTo(BigDecimal.ZERO) >= 0 && confidence.compareTo(BigDecimal.ONE) <= 0) {
                prediction.setConfidenceLevel(confidence);
            }
        }
        if (updates.getRecommendation() != null && !updates.getRecommendation().isEmpty()) {
            prediction.setFactorsApplied(updates.getRecommendation());
        }
        prediction.setUpdatedAt(LocalDateTime.now());
        DemandPrediction updated = predictionRepository.save(prediction);
        log.info("Prediction {} updated by pharmacy {}", predictionId, pharmacyId);
        Integer currentStock = getCurrentStock(updated.getProduct() != null ? updated.getProduct().getId() : null);
        return DemandPredictionResponse.fromEntity(updated, currentStock);
    }

    @Override
    @Transactional
    public void deletePrediction(Long predictionId, Long pharmacyId) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found: " + predictionId));
        if (!prediction.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied: Prediction does not belong to this pharmacy");
        }
        predictionRepository.delete(prediction);
        log.info("Prediction {} deleted by pharmacy {}", predictionId, pharmacyId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPredictionToPdf(Long predictionId, Long pharmacyId) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found"));
        if (!prediction.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }
        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        pdf.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        pdf.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        pdf.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>\nendobj\n");
        pdf.append("xref\n0 4\n0000000000 65535 f \n");
        pdf.append("trailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n10\n%%EOF");
        return pdf.toString().getBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPredictionToExcel(Long predictionId, Long pharmacyId) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found"));
        if (!prediction.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }
        StringBuilder excel = new StringBuilder();
        excel.append("Prediction ID,Product Name,Predicted Quantity,Current Stock,Recommended Order,Confidence\n");
        excel.append(prediction.getId()).append(",");
        excel.append(prediction.getProduct() != null ? prediction.getProduct().getName() : "N/A").append(",");
        excel.append(prediction.getPredictedQuantity()).append(",");
        excel.append(getCurrentStock(prediction.getProduct() != null ? prediction.getProduct().getId() : null)).append(",");
        excel.append(Math.max(0, prediction.getPredictedQuantity() - getCurrentStock(prediction.getProduct() != null ? prediction.getProduct().getId() : null))).append(",");
        excel.append(prediction.getConfidenceLevel() != null ? prediction.getConfidenceLevel() : "N/A");
        return excel.toString().getBytes();
    }

    @Override
    @Transactional
    public ShareLinkDTO generateShareLink(Long predictionId, Long pharmacyId, int expiryHours) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found"));
        if (!prediction.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expiryHours);
        String shareUrl = "https://smartpharma.app/share/prediction/" + token;
        return ShareLinkDTO.builder()
                .shareUrl(shareUrl)
                .expiresAt(expiresAt)
                .token(token)
                .build();
    }

    @Override
    @Transactional
    public PurchaseOrderSummaryDTO createPurchaseFromPrediction(Long predictionId, Long pharmacyId, Long userId) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found: " + predictionId));

        if (!prediction.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied: Prediction does not belong to this pharmacy");
        }

        Integer predictedQty = prediction.getPredictedQuantity() != null ? prediction.getPredictedQuantity() : 0;
        Integer currentStock = getCurrentStock(prediction.getProduct() != null ? prediction.getProduct().getId() : null);
        Integer recommendedOrder = Math.max(0, predictedQty - currentStock);

        // ✅ FIXED: Return success response instead of throwing exception
        if (recommendedOrder <= 0) {
            return PurchaseOrderSummaryDTO.builder()
                    .purchaseOrderId(predictionId)
                    .orderNumber(null)
                    .productId(prediction.getProduct() != null ? prediction.getProduct().getId() : null)
                    .productName(prediction.getProduct() != null ? prediction.getProduct().getName() : "N/A")
                    .quantity(0)
                    .status("NO_ORDER_NEEDED")
                    .message("المخزون الحالي كافٍ - لا حاجة لطلب جديد")
                    .build();
        }

        return PurchaseOrderSummaryDTO.builder()
                .purchaseOrderId(predictionId)
                .orderNumber("PO-" + System.currentTimeMillis())
                .productId(prediction.getProduct() != null ? prediction.getProduct().getId() : null)
                .productName(prediction.getProduct() != null ? prediction.getProduct().getName() : "N/A")
                .quantity(recommendedOrder)
                .status("DRAFT")
                .message("Purchase order created from prediction")
                .build();
    }
    private int calculateMovingAverage(List<Integer> sales, int days) {
        if (sales == null || sales.isEmpty()) return 0;
        int sum = 0;
        int count = Math.min(sales.size(), days);
        for (int i = sales.size() - 1; i >= Math.max(0, sales.size() - days); i--) {
            sum += sales.get(i);
        }
        return count > 0 ? sum / count : 0;
    }

    private BigDecimal getSeasonalityMultiplier(LocalDate date, String category) {
        if (date == null) return BigDecimal.ONE;
        int month = date.getMonthValue();
        Map<String, Map<Integer, BigDecimal>> seasonalityMap = Map.of(
                "مسكنات", Map.of(12, BigDecimal.valueOf(1.3), 1, BigDecimal.valueOf(1.4), 2, BigDecimal.valueOf(1.2)),
                "مضادات حيوية", Map.of(10, BigDecimal.valueOf(1.2), 11, BigDecimal.valueOf(1.3), 12, BigDecimal.valueOf(1.4)),
                "فيتامينات", Map.of(9, BigDecimal.valueOf(1.2), 10, BigDecimal.valueOf(1.3)),
                "حساسية", Map.of(3, BigDecimal.valueOf(1.3), 4, BigDecimal.valueOf(1.4), 5, BigDecimal.valueOf(1.2))
        );
        Map<Integer, BigDecimal> monthFactors = seasonalityMap.getOrDefault(category, Map.of());
        return monthFactors.getOrDefault(month, BigDecimal.ONE);
    }

    private BigDecimal calculateTrendFactor(List<Integer> sales) {
        if (sales == null || sales.size() < 7) return BigDecimal.ONE;
        int recentAvg = calculateMovingAverage(sales, 7);
        List<Integer> olderSales = sales.subList(0, Math.min(sales.size(), 14));
        int olderAvg = calculateMovingAverage(olderSales, 7);
        if (olderAvg == 0) return BigDecimal.ONE;
        BigDecimal ratio = BigDecimal.valueOf(recentAvg)
                .divide(BigDecimal.valueOf(olderAvg), 2, BigDecimal.ROUND_HALF_UP);
        return ratio.max(BigDecimal.valueOf(0.8)).min(BigDecimal.valueOf(1.3));
    }

    private BigDecimal calculateConfidence(List<Integer> historicalSales, Integer predicted) {
        if (historicalSales == null || historicalSales.size() < 7) {
            return CONFIDENCE_BASE.multiply(BigDecimal.valueOf(0.8));
        }
        BigDecimal dataConfidence = BigDecimal.valueOf(Math.min(1.0, historicalSales.size() / 30.0));
        double variance = calculateVariance(historicalSales);
        BigDecimal varianceConfidence = BigDecimal.valueOf(Math.max(0.5, 1.0 - (variance / 100.0)));
        return CONFIDENCE_BASE.multiply(dataConfidence)
                .multiply(varianceConfidence)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private double calculateVariance(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0;
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        return values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
    }

    private String buildFactorsJson(LocalDate date, String category) {
        Map<String, Object> factors = new HashMap<>();
        factors.put("seasonality", getSeasonalityMultiplier(date, category).doubleValue());
        factors.put("trend", "calculated");
        factors.put("dayOfWeek", date != null ? date.getDayOfWeek().name() : "UNKNOWN");
        factors.put("movingAvgDays", MOVING_AVG_DAYS);
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(factors);
        } catch (Exception e) {
            return "{}";
        }
    }

    private List<Integer> getHistoricalSales(Long productId, Long pharmacyId, int days) {
        if (productId == null || pharmacyId == null) return Collections.emptyList();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<SaleItem> saleItems = saleItemRepository.findByProductIdAndPharmacyIdAndDateBetween(
                productId, pharmacyId, startDateTime, endDateTime);
        if (saleItems == null || saleItems.isEmpty()) return Collections.emptyList();
        Map<LocalDate, Integer> dailySales = saleItems.stream()
                .filter(item -> item.getTransaction() != null && item.getTransaction().getTransactionDate() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getTransaction().getTransactionDate().toLocalDate(),
                        Collectors.summingInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                ));
        List<Integer> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            result.add(dailySales.getOrDefault(date, 0));
        }
        return result;
    }

    private Integer getCurrentStock(Long productId) {
        if (productId == null) return 0;
        Product product = productRepository.findById(productId).orElse(null);
        return product != null && product.getTotalStock() != null ? product.getTotalStock() : 0;
    }
}