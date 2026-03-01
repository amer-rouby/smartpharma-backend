package com.smartpharma.service.impl;

import com.smartpharma.dto.response.DemandPredictionResponse;
import com.smartpharma.entity.*;
import com.smartpharma.repository.*;
import com.smartpharma.service.DemandPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DemandPredictionService.
 * Provides demand forecasting using simple moving average algorithm.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemandPredictionServiceImpl implements DemandPredictionService {

    private final DemandPredictionRepository predictionRepository;
    private final ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;
    private final SaleItemRepository saleItemRepository;
    private final StockBatchRepository stockBatchRepository;

    // Algorithm configuration constants
    private static final int MOVING_AVG_DAYS = 14;
    private static final int DEFAULT_PREDICTION = 10;

    /**
     * Generate predictions for all products in a pharmacy.
     * Each product prediction is processed in isolated transaction.
     */
    @Override
    @Transactional
    public void generatePredictions(Long pharmacyId, LocalDate forDate) {
        log.info("Starting prediction generation for pharmacy: {}, date: {}", pharmacyId, forDate);

        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found with ID: " + pharmacyId));

        List<Product> products = productRepository.findByPharmacyId(pharmacyId);
        int success = 0, failed = 0;

        // Process each product independently (isolated transactions)
        for (Product product : products) {
            try {
                saveSingleProductPrediction(product, pharmacy, forDate);
                success++;
            } catch (Exception e) {
                log.error("Failed for product ID {}: {}", product.getId(), e.getMessage(), e);
                failed++;
            }
        }
        log.info("Completed. Success: {}, Failed: {}", success, failed);
    }

    /**
     * Save prediction for single product (isolated transaction).
     * Uses REQUIRES_NEW to prevent one failure from stopping others.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSingleProductPrediction(Product product, Pharmacy pharmacy, LocalDate forDate) {
        List<Integer> historicalSales = getHistoricalSales(product.getId(), pharmacy.getId(), 30);
        Integer predictedQty = calculateSimpleForecast(historicalSales, forDate, product.getCategory());

        // Ensure valid prediction value (never null or zero)
        if (predictedQty == null || predictedQty < 1) {
            predictedQty = DEFAULT_PREDICTION;
        }

        // Check if prediction already exists for upsert
        Optional<DemandPrediction> existingOpt = predictionRepository
                .findByProductIdAndPharmacyIdAndPredictionDate(
                        product.getId(), pharmacy.getId(), forDate);

        DemandPrediction prediction;
        if (existingOpt.isPresent()) {
            // Update existing
            prediction = existingOpt.get();
            prediction.setPredictedQuantity(predictedQty);
            prediction.setAlgorithmVersion("v1-sma");
            prediction.setConfidenceLevel(BigDecimal.valueOf(0.75));
            prediction.setFactorsApplied(buildFactorsJson(forDate, product.getCategory()));
            prediction.setUpdatedAt(LocalDateTime.now());
        } else {
            // Create new
            prediction = DemandPrediction.builder()
                    .product(product)
                    .pharmacy(pharmacy)
                    .predictionDate(forDate)
                    .predictedQuantity(predictedQty)
                    .algorithmVersion("v1-sma")
                    .confidenceLevel(BigDecimal.valueOf(0.75))
                    .factorsApplied(buildFactorsJson(forDate, product.getCategory()))
                    .isConsumed(false)
                    .build();
        }

        predictionRepository.save(prediction);
        log.info("Saved prediction: product={}, date={}, qty={}",
                product.getId(), forDate, predictedQty);
    }

    /**
     * Simple forecasting algorithm (MVP version).
     * Uses moving average + weekend boost factor.
     */
    @Override
    public Integer calculateSimpleForecast(List<Integer> historicalSales,
                                           LocalDate predictionDate,
                                           String category) {
        if (historicalSales == null || historicalSales.isEmpty()) {
            return DEFAULT_PREDICTION;
        }

        // Calculate simple moving average
        double avg = historicalSales.stream()
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(DEFAULT_PREDICTION);

        // Apply weekend boost factor (Friday = higher demand)
        double multiplier = 1.0;
        if (predictionDate != null) {
            DayOfWeek day = predictionDate.getDayOfWeek();
            if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) {
                multiplier = 1.2;
            }
        }

        return (int) Math.max(1, Math.round(avg * multiplier));
    }

    /**
     * Calculate current stock from active stock batches.
     * Sums quantityCurrent from all ACTIVE batches for product.
     */
    private Integer getCurrentStock(Long productId) {
        if (productId == null) return 0;

        List<StockBatch> activeBatches = stockBatchRepository
                .findByProductIdAndStatusActive(productId);

        if (activeBatches == null || activeBatches.isEmpty()) {
            return 0;
        }

        return activeBatches.stream()
                .mapToInt(batch ->
                        batch.getQuantityCurrent() != null ? batch.getQuantityCurrent() : 0)
                .sum();
    }

    /**
     * Fetch historical sales data for forecasting.
     * Returns daily sales quantities for last N days.
     */
    private List<Integer> getHistoricalSales(Long productId, Long pharmacyId, int days) {
        if (productId == null || pharmacyId == null) {
            return Collections.emptyList();
        }

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        // Query sales items with date range
        List<SaleItem> items = saleItemRepository.findByProductIdAndPharmacyIdAndDateBetween(
                productId, pharmacyId, start.atStartOfDay(), end.atTime(LocalTime.MAX));

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        // Group by date and sum quantities
        Map<LocalDate, Integer> daily = items.stream()
                .filter(i -> i.getTransaction() != null &&
                        i.getTransaction().getTransactionDate() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getTransaction().getTransactionDate().toLocalDate(),
                        Collectors.summingInt(i ->
                                Optional.ofNullable(i.getQuantity()).orElse(0))));

        // Fill missing dates with zero (ensure continuous data)
        List<Integer> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            result.add(daily.getOrDefault(d, 0));
        }
        return result;
    }

    /**
     * Build JSON string for factors applied in prediction.
     * Used for debugging and algorithm transparency.
     */
    private String buildFactorsJson(LocalDate date, String category) {
        String day = date != null ? date.getDayOfWeek().toString() : "UNKNOWN";
        String cat = category != null ? category : "UNKNOWN";
        return String.format("{\"day\":\"%s\", \"category\":\"%s\"}", day, cat);
    }

    /**
     * Get paginated predictions list for a pharmacy.
     * Includes real-time current stock calculation.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DemandPredictionResponse> getPredictions(Long pharmacyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return predictionRepository.findByPharmacyId(pharmacyId, pageable)
                .map(pred -> {
                    Integer currentStock = getCurrentStock(
                            pred.getProduct() != null ? pred.getProduct().getId() : null);
                    return DemandPredictionResponse.fromEntity(pred, currentStock);
                });
    }

    /**
     * Update prediction with actual sales data.
     * Calculates accuracy percentage after update.
     */
    @Override
    @Transactional
    public void updatePredictionWithActual(Long id, Integer actual) {
        predictionRepository.findById(id).ifPresent(p -> {
            p.setActualQuantity(actual);
            p.setAccuracyPercentage(p.calculateAccuracy());
            predictionRepository.save(p);
        });
    }

    /**
     * Get upcoming predictions for next N days.
     * Filters out realized predictions (with actual sales).
     */
    @Override
    @Transactional(readOnly = true)
    public List<DemandPredictionResponse> getUpcomingPredictions(Long pharmacyId, int days) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(days);

        List<DemandPrediction> predictions = predictionRepository
                .findUpcomingPredictions(pharmacyId, start, end);

        if (predictions == null || predictions.isEmpty()) {
            return Collections.emptyList();
        }

        // Map with real-time current stock calculation
        return predictions.stream()
                .map(pred -> {
                    Integer currentStock = getCurrentStock(
                            pred.getProduct() != null ? pred.getProduct().getId() : null);
                    return DemandPredictionResponse.fromEntity(pred, currentStock);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get accuracy statistics for pharmacy dashboard.
     * Returns total predictions, average accuracy, and last updated date.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAccuracyStats(Long pharmacyId) {
        Long totalPredictions = predictionRepository.countByPharmacyId(pharmacyId);
        BigDecimal averageAccuracy = predictionRepository
                .calculateAverageAccuracyByPharmacy(pharmacyId);
        String lastUpdated = predictionRepository
                .findLatestPredictionDateByPharmacy(pharmacyId)
                .map(LocalDate::toString)
                .orElse(LocalDate.now().toString());

        return Map.of(
                "totalPredictions", totalPredictions != null ? totalPredictions : 0L,
                "averageAccuracy", averageAccuracy != null ? averageAccuracy.doubleValue() : 0.0,
                "lastUpdated", lastUpdated
        );
    }

    /**
     * Generate prediction for single product (public API method).
     * Delegates to internal save method with proper error handling.
     */
    @Override
    @Transactional
    public DemandPredictionResponse generatePredictionForProduct(Long productId, Long pharmacyId, LocalDate predictionDate) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found with ID: " + pharmacyId));

        saveSingleProductPrediction(product, pharmacy, predictionDate);

        // Return the created/updated prediction
        return predictionRepository
                .findByProductIdAndPharmacyIdAndPredictionDate(productId, pharmacyId, predictionDate)
                .map(pred -> {
                    Integer currentStock = getCurrentStock(productId);
                    return DemandPredictionResponse.fromEntity(pred, currentStock);
                })
                .orElse(null);
    }
}