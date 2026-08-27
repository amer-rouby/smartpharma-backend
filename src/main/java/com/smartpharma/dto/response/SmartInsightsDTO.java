package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartInsightsDTO {
    private BigDecimal todayRevenue;
    private BigDecimal averageDailyRevenue30d;
    private Double salesDeltaPercent;

    // Each of these is null when its underlying smart feature is disabled
    // for the pharmacy, rather than shown as zero - "no data" vs "off".
    private Integer highRiskStockoutCount;
    private Integer reorderRecommendationsCount;
    private Integer pricingRecommendationsCount;

    // Wired once anomaly/unusual-activity detection exists; null until then,
    // same as any other sub-metric whose feature isn't available.
    private Integer unusualActivityCount;
}
