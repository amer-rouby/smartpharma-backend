package com.smartpharma.service.impl;

import com.smartpharma.dto.response.AnomalyDetectionResponse;
import com.smartpharma.entity.AnomalyDetection;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.SaleTransaction;
import com.smartpharma.entity.StockAdjustmentHistory;
import com.smartpharma.entity.User;
import com.smartpharma.exception.LocalizedException;
import com.smartpharma.repository.AnomalyDetectionRepository;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.PurchaseOrderRepository;
import com.smartpharma.repository.SaleTransactionRepository;
import com.smartpharma.repository.StockAdjustmentHistoryRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.service.AnomalyDetectionService;
import com.smartpharma.service.settings.SmartFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

// Rule-based only, no ML. Every check compares real recent activity against
// either a fixed threshold or the user's own historical average - never
// against other users, and never labeled as fraud. Detected rows are surfaced
// for a pharmacist/admin to review and mark reviewed/dismissed; nothing here
// blocks a user or an action automatically.
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {

    private final AnomalyDetectionRepository anomalyRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockAdjustmentHistoryRepository stockAdjustmentHistoryRepository;
    private final SmartFeatureSettingsService smartFeatureSettingsService;

    private static final int WINDOW_HOURS = 24;
    private static final int EXCESSIVE_RETURNS_THRESHOLD = 3;
    private static final int EXCESSIVE_RETURNS_HIGH_THRESHOLD = 6;
    private static final int FREQUENT_CANCELLATIONS_THRESHOLD = 3;
    private static final int FREQUENT_CANCELLATIONS_HIGH_THRESHOLD = 6;
    private static final int REPEATED_ADJUSTMENTS_THRESHOLD = 5;
    private static final int REPEATED_ADJUSTMENTS_HIGH_THRESHOLD = 10;
    private static final int LARGE_DISCREPANCY_MIN_DELTA = 50;
    private static final int LARGE_DISCREPANCY_HIGH_DELTA = 100;
    private static final int MIN_HISTORY_FOR_DISCOUNT_BASELINE = 5;
    private static final double DISCOUNT_STD_DEV_THRESHOLD = 2.0;

    @Override
    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void runDetectionForAllPharmacies() {
        log.info("Running scheduled anomaly detection...");
        for (Pharmacy pharmacy : pharmacyRepository.findByDeletedAtIsNull()) {
            try {
                runDetectionForPharmacy(pharmacy.getId());
            } catch (Exception e) {
                log.error("Error running anomaly detection for pharmacy {}: {}", pharmacy.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void runDetectionForPharmacy(Long pharmacyId) {
        Boolean enabled = smartFeatureSettingsService.getOrCreate(pharmacyId).getAnomalyDetectionEnabled();
        if (enabled != null && !enabled) {
            return;
        }

        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId).orElse(null);
        if (pharmacy == null) return;

        LocalDateTime since = LocalDateTime.now().minusHours(WINDOW_HOURS);

        checkExcessiveReturns(pharmacy, since);
        checkFrequentCancellations(pharmacy, since);
        checkRepeatedStockAdjustments(pharmacy, since);
        checkLargeStockDiscrepancies(pharmacy, since);
        checkUnusualDiscounts(pharmacy, since);
    }

    private void checkExcessiveReturns(Pharmacy pharmacy, LocalDateTime since) {
        List<Object[]> rows = saleTransactionRepository.countDeletedSalesByUserSince(pharmacy.getId(), since);
        for (Object[] row : rows) {
            Long userId = ((Number) row[0]).longValue();
            String userName = (String) row[1];
            long count = ((Number) row[2]).longValue();
            if (count < EXCESSIVE_RETURNS_THRESHOLD) continue;

            if (isDuplicate(pharmacy.getId(), AnomalyDetection.Type.EXCESSIVE_RETURNS, "USER", userId, since)) continue;

            AnomalyDetection.Severity severity = count >= EXCESSIVE_RETURNS_HIGH_THRESHOLD
                    ? AnomalyDetection.Severity.HIGH : AnomalyDetection.Severity.MEDIUM;

            create(pharmacy, AnomalyDetection.Type.EXCESSIVE_RETURNS, severity,
                    "Unusual activity detected: " + userName + " had " + count
                            + " sale(s) cancelled/returned in the last 24 hours, more than usual.",
                    "USER", userId, userName);
        }
    }

    private void checkFrequentCancellations(Pharmacy pharmacy, LocalDateTime since) {
        List<Object[]> rows = purchaseOrderRepository.countCancelledOrdersByUserSince(pharmacy.getId(), since);
        for (Object[] row : rows) {
            Long userId = ((Number) row[0]).longValue();
            String userName = (String) row[1];
            long count = ((Number) row[2]).longValue();
            if (count < FREQUENT_CANCELLATIONS_THRESHOLD) continue;

            if (isDuplicate(pharmacy.getId(), AnomalyDetection.Type.FREQUENT_CANCELLATIONS, "USER", userId, since)) continue;

            AnomalyDetection.Severity severity = count >= FREQUENT_CANCELLATIONS_HIGH_THRESHOLD
                    ? AnomalyDetection.Severity.HIGH : AnomalyDetection.Severity.MEDIUM;

            create(pharmacy, AnomalyDetection.Type.FREQUENT_CANCELLATIONS, severity,
                    "Unusual activity detected: " + userName + " cancelled " + count
                            + " purchase order(s) in the last 24 hours, more than usual.",
                    "USER", userId, userName);
        }
    }

    private void checkRepeatedStockAdjustments(Pharmacy pharmacy, LocalDateTime since) {
        List<Object[]> rows = stockAdjustmentHistoryRepository.countAdjustmentsByUserSince(pharmacy.getId(), since);
        for (Object[] row : rows) {
            Long userId = ((Number) row[0]).longValue();
            String userName = (String) row[1];
            long count = ((Number) row[2]).longValue();
            if (count < REPEATED_ADJUSTMENTS_THRESHOLD) continue;

            if (isDuplicate(pharmacy.getId(), AnomalyDetection.Type.REPEATED_STOCK_ADJUSTMENTS, "USER", userId, since)) continue;

            AnomalyDetection.Severity severity = count >= REPEATED_ADJUSTMENTS_HIGH_THRESHOLD
                    ? AnomalyDetection.Severity.HIGH : AnomalyDetection.Severity.MEDIUM;

            create(pharmacy, AnomalyDetection.Type.REPEATED_STOCK_ADJUSTMENTS, severity,
                    "Unusual activity detected: " + userName + " made " + count
                            + " stock adjustment(s) in the last 24 hours, more than usual.",
                    "USER", userId, userName);
        }
    }

    private void checkLargeStockDiscrepancies(Pharmacy pharmacy, LocalDateTime since) {
        List<StockAdjustmentHistory> rows = stockAdjustmentHistoryRepository
                .findLargeDiscrepanciesSince(pharmacy.getId(), since, LARGE_DISCREPANCY_MIN_DELTA);

        for (StockAdjustmentHistory h : rows) {
            if (isDuplicate(pharmacy.getId(), AnomalyDetection.Type.LARGE_STOCK_DISCREPANCY, "STOCK_ADJUSTMENT", h.getId(), null)) continue;

            int delta = Math.abs(h.getNewQuantity() - h.getPreviousQuantity());
            AnomalyDetection.Severity severity = delta >= LARGE_DISCREPANCY_HIGH_DELTA
                    ? AnomalyDetection.Severity.HIGH : AnomalyDetection.Severity.MEDIUM;

            String batchNumber = h.getBatch() != null ? h.getBatch().getBatchNumber() : "?";
            create(pharmacy, AnomalyDetection.Type.LARGE_STOCK_DISCREPANCY, severity,
                    "Unusual activity detected: stock adjustment for batch " + batchNumber
                            + " changed quantity from " + h.getPreviousQuantity() + " to " + h.getNewQuantity()
                            + " (" + delta + " unit(s)), larger than usual.",
                    "STOCK_ADJUSTMENT", h.getId(), batchNumber);
        }
    }

    private void checkUnusualDiscounts(Pharmacy pharmacy, LocalDateTime since) {
        List<SaleTransaction> recentSales = saleTransactionRepository
                .findByPharmacyIdAndTransactionDateBetween(pharmacy.getId(), since, LocalDateTime.now());

        for (SaleTransaction sale : recentSales) {
            if (sale.getUser() == null || sale.getSubtotal() == null
                    || sale.getSubtotal().compareTo(BigDecimal.ZERO) <= 0) continue;

            double saleDiscountPct = discountPercent(sale);
            if (saleDiscountPct <= 0) continue;

            List<SaleTransaction> history = saleTransactionRepository.findByPharmacyIdAndUserIdAndTransactionDateAfter(
                    pharmacy.getId(), sale.getUser().getId(), LocalDateTime.now().minusDays(30));
            history.removeIf(s -> s.getId().equals(sale.getId()));

            if (history.size() < MIN_HISTORY_FOR_DISCOUNT_BASELINE) continue;

            double[] pcts = history.stream()
                    .filter(s -> s.getSubtotal() != null && s.getSubtotal().compareTo(BigDecimal.ZERO) > 0)
                    .mapToDouble(this::discountPercent)
                    .toArray();
            if (pcts.length < MIN_HISTORY_FOR_DISCOUNT_BASELINE) continue;

            double mean = 0;
            for (double p : pcts) mean += p;
            mean /= pcts.length;

            double variance = 0;
            for (double p : pcts) variance += Math.pow(p - mean, 2);
            variance /= pcts.length;
            double stdDev = Math.sqrt(variance);

            double threshold = mean + DISCOUNT_STD_DEV_THRESHOLD * stdDev;
            if (stdDev <= 0 || saleDiscountPct <= threshold) continue;

            if (isDuplicate(pharmacy.getId(), AnomalyDetection.Type.UNUSUAL_DISCOUNT, "SALE", sale.getId(), null)) continue;

            String userName = sale.getUser().getFullName();
            create(pharmacy, AnomalyDetection.Type.UNUSUAL_DISCOUNT, AnomalyDetection.Severity.MEDIUM,
                    "Unusual activity detected: a " + round1(saleDiscountPct) + "% discount on sale "
                            + (sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : ("#" + sale.getId()))
                            + " by " + userName + " is well above their usual average of " + round1(mean) + "%.",
                    "SALE", sale.getId(), sale.getInvoiceNumber());
        }
    }

    private double discountPercent(SaleTransaction sale) {
        if (sale.getSubtotal() == null || sale.getSubtotal().compareTo(BigDecimal.ZERO) <= 0
                || sale.getDiscountAmount() == null) return 0;
        return sale.getDiscountAmount()
                .divide(sale.getSubtotal(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private boolean isDuplicate(Long pharmacyId, AnomalyDetection.Type type, String relatedEntityType,
                                  Long relatedEntityId, LocalDateTime since) {
        LocalDateTime effectiveSince = since != null ? since : LocalDateTime.now().minusYears(10);
        return !anomalyRepository.findRecentDuplicates(pharmacyId, type, relatedEntityType, relatedEntityId, effectiveSince).isEmpty();
    }

    private void create(Pharmacy pharmacy, AnomalyDetection.Type type, AnomalyDetection.Severity severity,
                          String description, String relatedEntityType, Long relatedEntityId, String relatedEntityName) {
        AnomalyDetection anomaly = AnomalyDetection.builder()
                .pharmacy(pharmacy)
                .type(type)
                .severity(severity)
                .status(AnomalyDetection.Status.NEW)
                .description(description)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .relatedEntityName(relatedEntityName)
                .build();
        anomalyRepository.save(anomaly);
        log.info("Anomaly detected: pharmacy={}, type={}, severity={}", pharmacy.getId(), type, severity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnomalyDetectionResponse> getAnomalies(Long pharmacyId, AnomalyDetection.Status status,
                                                          AnomalyDetection.Type type, int page, int size) {
        Boolean enabled = smartFeatureSettingsService.getOrCreate(pharmacyId).getAnomalyDetectionEnabled();
        if (enabled != null && !enabled) {
            throw new LocalizedException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_ANOMALY_DETECTION",
                    "Anomaly detection feature is disabled for this pharmacy");
        }
        PageRequest pageable = PageRequest.of(page, size);
        Page<AnomalyDetection> result;
        if (status != null && type != null) {
            result = anomalyRepository.findByPharmacyIdAndStatusAndTypeOrderByDetectedAtDesc(pharmacyId, status, type, pageable);
        } else if (status != null) {
            result = anomalyRepository.findByPharmacyIdAndStatusOrderByDetectedAtDesc(pharmacyId, status, pageable);
        } else if (type != null) {
            result = anomalyRepository.findByPharmacyIdAndTypeOrderByDetectedAtDesc(pharmacyId, type, pageable);
        } else {
            result = anomalyRepository.findByPharmacyIdOrderByDetectedAtDesc(pharmacyId, pageable);
        }
        return result.map(AnomalyDetectionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(Long pharmacyId, AnomalyDetection.Status status) {
        return anomalyRepository.countByPharmacyIdAndStatus(pharmacyId, status);
    }

    @Override
    @Transactional
    public AnomalyDetectionResponse markReviewed(Long id, Long pharmacyId, Long userId) {
        return updateStatus(id, pharmacyId, userId, AnomalyDetection.Status.REVIEWED);
    }

    @Override
    @Transactional
    public AnomalyDetectionResponse dismiss(Long id, Long pharmacyId, Long userId) {
        return updateStatus(id, pharmacyId, userId, AnomalyDetection.Status.DISMISSED);
    }

    private AnomalyDetectionResponse updateStatus(Long id, Long pharmacyId, Long userId, AnomalyDetection.Status status) {
        AnomalyDetection anomaly = anomalyRepository.findByIdAndPharmacyId(id, pharmacyId)
                .orElseThrow(() -> new LocalizedException(HttpStatus.NOT_FOUND, "ANOMALY_NOT_FOUND",
                        "Anomaly not found: " + id));
        anomaly.setStatus(status);
        anomaly.setReviewedAt(LocalDateTime.now());
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            anomaly.setReviewedBy(user);
        }
        AnomalyDetection saved = anomalyRepository.save(anomaly);
        return AnomalyDetectionResponse.fromEntity(saved);
    }
}
