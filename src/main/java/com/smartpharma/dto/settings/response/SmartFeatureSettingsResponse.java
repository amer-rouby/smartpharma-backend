package com.smartpharma.dto.settings.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartpharma.entity.settings.SmartFeatureSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartFeatureSettingsResponse {

    private Long id;
    private Long pharmacyId;
    private Boolean stockPredictionEnabled;
    private Boolean reorderRecommendationsEnabled;
    private Boolean pricingRecommendationsEnabled;
    private Boolean supplierRecommendationsEnabled;
    private Boolean dashboardInsightsEnabled;
    private Boolean dailyBriefEnabled;
    private Boolean anomalyDetectionEnabled;
    private Boolean realtimeUpdatesEnabled;
    private Boolean voiceSearchEnabled;
    private Boolean aiAssistantEnabled;

    // Jackson's bean-introspection mangles getEInvoiceEnabled() (two capitals
    // right after "get") into "einvoiceEnabled" instead of "eInvoiceEnabled" -
    // pinned explicitly rather than relying on it.
    @JsonProperty("eInvoiceEnabled")
    private Boolean eInvoiceEnabled;
    private Boolean offlineModeEnabled;
    private LocalDateTime updatedAt;

    public static SmartFeatureSettingsResponse fromEntity(SmartFeatureSettings s) {
        return SmartFeatureSettingsResponse.builder()
                .id(s.getId())
                .pharmacyId(s.getPharmacy().getId())
                .stockPredictionEnabled(s.getStockPredictionEnabled())
                .reorderRecommendationsEnabled(s.getReorderRecommendationsEnabled())
                .pricingRecommendationsEnabled(s.getPricingRecommendationsEnabled())
                .supplierRecommendationsEnabled(s.getSupplierRecommendationsEnabled())
                .dashboardInsightsEnabled(s.getDashboardInsightsEnabled())
                .dailyBriefEnabled(s.getDailyBriefEnabled())
                .anomalyDetectionEnabled(s.getAnomalyDetectionEnabled())
                .realtimeUpdatesEnabled(s.getRealtimeUpdatesEnabled())
                .voiceSearchEnabled(s.getVoiceSearchEnabled())
                .aiAssistantEnabled(s.getAiAssistantEnabled())
                .eInvoiceEnabled(s.getEInvoiceEnabled())
                .offlineModeEnabled(s.getOfflineModeEnabled())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
