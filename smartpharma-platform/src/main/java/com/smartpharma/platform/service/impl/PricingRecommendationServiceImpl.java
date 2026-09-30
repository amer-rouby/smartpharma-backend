package com.smartpharma.platform.service.impl;

import com.smartpharma.platform.dto.response.PricingRecommendationDTO;
import com.smartpharma.catalog.entity.Product;
import com.smartpharma.catalog.entity.StockBatch;
import com.smartpharma.common.exception.LocalizedException;
import com.smartpharma.catalog.repository.ProductRepository;
import com.smartpharma.sales.repository.SaleItemRepository;
import com.smartpharma.catalog.repository.StockBatchRepository;
import com.smartpharma.platform.service.PricingRecommendationService;
import com.smartpharma.settings.service.SmartFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Suggestion-only: never writes to Product.sellPrice or any batch field. The
// pharmacist decides whether to apply a discount; this only surfaces which
// batches are worth a look, based on real expiry dates and real sale velocity.
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingRecommendationServiceImpl implements PricingRecommendationService {

    private final StockBatchRepository stockBatchRepository;
    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;
    private final SmartFeatureSettingsService smartFeatureSettingsService;

    private static final int URGENT_EXPIRY_DAYS = 7;
    private static final int WARNING_EXPIRY_DAYS = 30;
    private static final int URGENT_DISCOUNT_PERCENT = 30;
    private static final int WARNING_DISCOUNT_PERCENT = 15;
    private static final int SLOW_MOVER_DISCOUNT_PERCENT = 10;
    private static final int DEAD_STOCK_DISCOUNT_PERCENT = 20;
    private static final int VELOCITY_BASELINE_DAYS = 90;
    private static final int VELOCITY_RECENT_DAYS = 30;
    private static final double SLOW_MOVER_THRESHOLD_RATIO = 0.2;

    @Override
    @Transactional(readOnly = true)
    public List<PricingRecommendationDTO> getRecommendations(Long pharmacyId) {
        Boolean enabled = smartFeatureSettingsService.getOrCreate(pharmacyId).getPricingRecommendationsEnabled();
        if (enabled != null && !enabled) {
            throw new LocalizedException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_PRICING_RECOMMENDATIONS",
                    "Pricing recommendations feature is disabled for this pharmacy");
        }

        List<PricingRecommendationDTO> result = new ArrayList<>();
        result.addAll(buildExpiryRecommendations(pharmacyId));
        result.addAll(buildSlowMoverRecommendations(pharmacyId));

        result.sort(Comparator.comparingInt(r -> priorityRank(r.getPriority())));
        return result;
    }

    private List<PricingRecommendationDTO> buildExpiryRecommendations(Long pharmacyId) {
        List<PricingRecommendationDTO> recommendations = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(WARNING_EXPIRY_DAYS);
        List<StockBatch> batches = stockBatchRepository.findExpiringBatches(pharmacyId, threshold);

        for (StockBatch batch : batches) {
            if (batch.getQuantityCurrent() == null || batch.getQuantityCurrent() <= 0) continue;
            if (batch.getProduct() == null) continue;

            long daysUntilExpiry = ChronoUnit.DAYS.between(today, batch.getExpiryDate());
            int discountPercent;
            String priority;
            String message;

            if (daysUntilExpiry < URGENT_EXPIRY_DAYS) {
                discountPercent = URGENT_DISCOUNT_PERCENT;
                priority = "HIGH";
                message = "Expires in " + daysUntilExpiry + " day(s) - clear stock now";
            } else {
                discountPercent = WARNING_DISCOUNT_PERCENT;
                priority = "MEDIUM";
                message = "Expires in " + daysUntilExpiry + " day(s) - consider discounting soon";
            }

            recommendations.add(PricingRecommendationDTO.builder()
                    .productId(batch.getProduct().getId())
                    .productName(batch.getProduct().getName())
                    .productCode(batch.getProduct().getCode())
                    .batchId(batch.getId())
                    .batchNumber(batch.getBatchNumber())
                    .expiryDate(batch.getExpiryDate())
                    .daysUntilExpiry((int) daysUntilExpiry)
                    .currentStock(batch.getQuantityCurrent())
                    .reason("EXPIRING")
                    .suggestedDiscountPercent(discountPercent)
                    .priority(priority)
                    .message(message)
                    .build());
        }
        return recommendations;
    }

    private List<PricingRecommendationDTO> buildSlowMoverRecommendations(Long pharmacyId) {
        List<PricingRecommendationDTO> recommendations = new ArrayList<>();
        List<Product> products = productRepository.findByPharmacyId(pharmacyId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime baselineStart = now.minusDays(VELOCITY_BASELINE_DAYS).with(LocalTime.MIN);
        LocalDateTime recentStart = now.minusDays(VELOCITY_RECENT_DAYS).with(LocalTime.MIN);
        // Two grouped queries for all products instead of two per product.
        Map<Long, Long> soldInBaseline = soldPerProduct(pharmacyId, baselineStart, now);
        Map<Long, Long> soldRecently = soldPerProduct(pharmacyId, recentStart, now);

        for (Product product : products) {
            Integer currentStock = product.getTotalStock();
            if (currentStock == null || currentStock <= 0) continue;

            long sold90 = soldInBaseline.getOrDefault(product.getId(), 0L);
            if (sold90 == 0) {
                // Not a single sale in the whole window - the worst kind of
                // stagnant stock. Only flagged once the stock has been on the
                // shelf for the whole window, so a new arrival isn't "dead".
                LocalDateTime onShelfSince = oldestActiveBatch(product);
                if (onShelfSince != null && onShelfSince.isBefore(baselineStart)) {
                    recommendations.add(deadStock(product, currentStock));
                }
                continue;
            }
            int recentQuantity = soldRecently.getOrDefault(product.getId(), 0L).intValue();

            double avgMonthlyRate = sold90 / (VELOCITY_BASELINE_DAYS / 30.0);
            if (avgMonthlyRate <= 0 || recentQuantity >= avgMonthlyRate * SLOW_MOVER_THRESHOLD_RATIO) continue;

            recommendations.add(PricingRecommendationDTO.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productCode(product.getCode())
                    .currentStock(currentStock)
                    .reason("SLOW_MOVING")
                    .suggestedDiscountPercent(SLOW_MOVER_DISCOUNT_PERCENT)
                    .priority("LOW")
                    .message(String.format(
                            "Sold only %d unit(s) in the last %d days vs a %d-day average of %.1f/month",
                            recentQuantity, VELOCITY_RECENT_DAYS, VELOCITY_BASELINE_DAYS, avgMonthlyRate))
                    .build());
        }
        return recommendations;
    }

    private Map<Long, Long> soldPerProduct(Long pharmacyId, LocalDateTime start, LocalDateTime end) {
        Map<Long, Long> sold = new HashMap<>();
        for (Object[] row : saleItemRepository.sumQuantityPerProductByPharmacyIdAndDateRange(pharmacyId, start, end)) {
            sold.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return sold;
    }

    private static LocalDateTime oldestActiveBatch(Product product) {
        if (product.getStockBatches() == null) return null;
        return product.getStockBatches().stream()
                .filter(b -> b.getStatus() == StockBatch.BatchStatus.ACTIVE)
                .filter(b -> b.getQuantityCurrent() != null && b.getQuantityCurrent() > 0)
                .map(StockBatch::getCreatedAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private static PricingRecommendationDTO deadStock(Product product, int currentStock) {
        BigDecimal buyPrice = product.getBuyPrice() != null ? product.getBuyPrice() : BigDecimal.ZERO;
        BigDecimal tiedUp = buyPrice.multiply(BigDecimal.valueOf(currentStock));
        return PricingRecommendationDTO.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productCode(product.getCode())
                .currentStock(currentStock)
                .reason("DEAD_STOCK")
                .suggestedDiscountPercent(DEAD_STOCK_DISCOUNT_PERCENT)
                .priority("MEDIUM")
                .message(String.format("No sales in the last %d days - %d unit(s) in stock, %s tied up at cost",
                        VELOCITY_BASELINE_DAYS, currentStock, tiedUp.toPlainString()))
                .build();
    }

    private int priorityRank(String priority) {
        if (priority == null) return 3;
        return switch (priority) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            case "LOW" -> 2;
            default -> 3;
        };
    }
}
