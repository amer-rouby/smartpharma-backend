package com.smartpharma.service.impl;

import com.smartpharma.dto.response.AssistantAnswer;
import com.smartpharma.dto.response.DemandPredictionResponse;
import com.smartpharma.dto.response.ReorderRecommendationDTO;
import com.smartpharma.dto.response.SmartInsightsDTO;
import com.smartpharma.dto.response.StockBatchResponse;
import com.smartpharma.exception.LocalizedException;
import com.smartpharma.repository.SaleTransactionRepository;
import com.smartpharma.service.AssistantProvider;
import com.smartpharma.service.DashboardService;
import com.smartpharma.service.DemandPredictionService;
import com.smartpharma.service.StockBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

// Pattern-matches a fixed set of example questions (Arabic + English) and
// answers each by calling the real backend service behind it - never a
// fabricated or cached answer. Unmatched input gets an honest "try asking
// about X" fallback instead of guessing.
@Service
@RequiredArgsConstructor
@Slf4j
public class RuleBasedAssistantProvider implements AssistantProvider {

    private final SaleTransactionRepository saleTransactionRepository;
    private final DemandPredictionService demandPredictionService;
    private final StockBatchService stockBatchService;
    private final DashboardService dashboardService;

    private static final Set<String> NEAR_EXPIRY_KEYWORDS = Set.of("صلاحية", "الصلاحيه", "expir");
    private static final Set<String> REORDER_KEYWORDS = Set.of("اعادة طلب", "إعادة طلب", "اعادة الطلب", "إعادة الطلب", "تنصح", "reorder", "re-order");
    // "مبيع" alone is enough to catch the real question phrasing, since it
    // doesn't keep "أكثر"/"مبيع" adjacent (e.g. "أكثر المنتجات مبيعًا" has
    // "المنتجات" in between). Note "مبيع" is also a substring of "المبيعات"
    // (sales-vs-average question) - SALES_VS_AVERAGE is checked first below
    // to win that overlap.
    private static final Set<String> TOP_SELLING_KEYWORDS = Set.of("مبيع", "best sell", "top sell", "top product");
    private static final Set<String> SALES_VS_AVERAGE_KEYWORDS = Set.of("مقارنة", "متوسط", "compared", "vs average", "average");
    private static final Set<String> STOCKOUT_SOON_KEYWORDS = Set.of("تنفد", "نفاد", "ستنفذ", "ستنفد", "run out", "stock out", "stockout");

    @Override
    public AssistantAnswer answer(String query, Long pharmacyId) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        if (containsAny(normalized, NEAR_EXPIRY_KEYWORDS)) {
            return safely(pharmacyId, "NEAR_EXPIRY", () -> answerNearExpiry(pharmacyId));
        }
        if (containsAny(normalized, REORDER_KEYWORDS)) {
            return safely(pharmacyId, "REORDER", () -> answerReorder(pharmacyId));
        }
        if (containsAny(normalized, SALES_VS_AVERAGE_KEYWORDS)) {
            return safely(pharmacyId, "SALES_VS_AVERAGE", () -> answerSalesVsAverage(pharmacyId));
        }
        if (containsAny(normalized, TOP_SELLING_KEYWORDS)) {
            return safely(pharmacyId, "TOP_SELLING_WEEK", () -> answerTopSellingThisWeek(pharmacyId));
        }
        if (containsAny(normalized, STOCKOUT_SOON_KEYWORDS)) {
            return safely(pharmacyId, "STOCKOUT_SOON", () -> answerStockoutSoon(pharmacyId));
        }

        return AssistantAnswer.builder()
                .matched(false)
                .intent("UNMATCHED")
                .text("لم أفهم سؤالك. جرّب أن تسأل عن: أكثر المنتجات مبيعًا هذا الأسبوع، "
                        + "المنتجات التي ستنفد قريبًا، المنتجات القريبة من انتهاء الصلاحية، "
                        + "مبيعات اليوم مقارنة بالمتوسط، أو المنتجات الموصى بإعادة طلبها.")
                .build();
    }

    private boolean containsAny(String text, Set<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private AssistantAnswer safely(Long pharmacyId, String intent, java.util.function.Supplier<String> handler) {
        try {
            return AssistantAnswer.builder().matched(true).intent(intent).text(handler.get()).build();
        } catch (LocalizedException e) {
            return AssistantAnswer.builder().matched(true).intent(intent)
                    .text("هذه الميزة غير مفعّلة حاليًا لهذه الصيدلية.").build();
        } catch (Exception e) {
            log.warn("Assistant intent {} failed for pharmacy {}: {}", intent, pharmacyId, e.getMessage());
            return AssistantAnswer.builder().matched(true).intent(intent)
                    .text("حدث خطأ أثناء جلب البيانات، حاول مرة أخرى.").build();
        }
    }

    private String answerTopSellingThisWeek(Long pharmacyId) {
        LocalDate today = LocalDate.now();
        // today.with(DayOfWeek) adjusts within the ISO Monday-based week and can
        // land AFTER today (e.g. today=Friday, first-day=Sunday jumps to next
        // Sunday), producing an inverted start>end range. Compute the offset
        // back to the most recent occurrence of the locale's first day instead.
        java.time.DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
        int daysSinceStartOfWeek = (today.getDayOfWeek().getValue() - firstDayOfWeek.getValue() + 7) % 7;
        LocalDate startOfWeek = today.minusDays(daysSinceStartOfWeek);
        LocalDateTime start = startOfWeek.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        List<Object[]> rows = saleTransactionRepository.getTopProducts(pharmacyId, start, end, PageRequest.of(0, 5));
        if (rows.isEmpty()) {
            return "لا توجد مبيعات مسجلة هذا الأسبوع حتى الآن.";
        }

        StringBuilder sb = new StringBuilder("أكثر المنتجات مبيعًا هذا الأسبوع:\n");
        int rank = 1;
        for (Object[] row : rows) {
            String productName = (String) row[1];
            Number quantity = (Number) row[2];
            sb.append(rank++).append(". ").append(productName)
                    .append(" — ").append(quantity.intValue()).append(" وحدة\n");
        }
        return sb.toString().trim();
    }

    private String answerStockoutSoon(Long pharmacyId) {
        List<DemandPredictionResponse> predictions = demandPredictionService.getUpcomingPredictions(pharmacyId, 1);
        List<DemandPredictionResponse> atRisk = predictions.stream()
                .filter(p -> "HIGH".equals(p.getRiskLevel()) || "CRITICAL".equals(p.getRiskLevel()))
                .collect(Collectors.toList());

        if (atRisk.isEmpty()) {
            return "لا توجد منتجات في خطر نفاد مرتفع حاليًا.";
        }

        StringBuilder sb = new StringBuilder("منتجات قد تنفد قريبًا:\n");
        for (DemandPredictionResponse p : atRisk) {
            sb.append("- ").append(p.getProductName());
            if (p.getDaysUntilStockout() != null) {
                sb.append(" — متبقٍ حوالي ").append(p.getDaysUntilStockout()).append(" يوم/أيام");
            }
            sb.append(" (خطورة: ").append(p.getRiskLevel()).append(")\n");
        }
        return sb.toString().trim();
    }

    private String answerNearExpiry(Long pharmacyId) {
        List<StockBatchResponse> batches = stockBatchService.getExpiringBatches(pharmacyId, 30);
        if (batches.isEmpty()) {
            return "لا توجد أدوية قريبة من انتهاء الصلاحية خلال الـ 30 يومًا القادمة.";
        }

        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder("أدوية قريبة من انتهاء الصلاحية:\n");
        for (StockBatchResponse b : batches) {
            long daysLeft = b.getExpiryDate() != null ? ChronoUnit.DAYS.between(today, b.getExpiryDate()) : -1;
            sb.append("- ").append(b.getProductName())
                    .append(" (دفعة ").append(b.getBatchNumber()).append(")");
            if (daysLeft >= 0) {
                sb.append(" — تنتهي خلال ").append(daysLeft).append(" يوم/أيام");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String answerSalesVsAverage(Long pharmacyId) {
        SmartInsightsDTO insights = dashboardService.getSmartInsights(pharmacyId);
        String today = insights.getTodayRevenue() != null ? insights.getTodayRevenue().toString() : "0";
        String avg = insights.getAverageDailyRevenue30d() != null ? insights.getAverageDailyRevenue30d().toString() : "0";

        StringBuilder sb = new StringBuilder("مبيعات اليوم: ").append(today)
                .append(" جنيه، مقابل متوسط آخر 30 يوم: ").append(avg).append(" جنيه");

        if (insights.getSalesDeltaPercent() != null) {
            double delta = insights.getSalesDeltaPercent();
            sb.append(" (").append(delta >= 0 ? "أعلى بـ " : "أقل بـ ")
                    .append(String.format(Locale.ROOT, "%.1f", Math.abs(delta))).append("%)");
        }
        return sb.toString();
    }

    private String answerReorder(Long pharmacyId) {
        List<ReorderRecommendationDTO> recommendations = demandPredictionService.getReorderRecommendations(pharmacyId);
        if (recommendations.isEmpty()) {
            return "لا توجد منتجات موصى بإعادة طلبها حاليًا.";
        }

        StringBuilder sb = new StringBuilder("منتجات موصى بإعادة طلبها:\n");
        for (ReorderRecommendationDTO r : recommendations) {
            sb.append("- ").append(r.getProductName())
                    .append(" — الكمية المقترحة: ").append(r.getRecommendedQuantity());
            if (r.getSupplierName() != null) {
                sb.append(" (المورد: ").append(r.getSupplierName()).append(")");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }
}
