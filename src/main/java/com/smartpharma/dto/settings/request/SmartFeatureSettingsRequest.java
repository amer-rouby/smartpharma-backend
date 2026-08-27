package com.smartpharma.dto.settings.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartFeatureSettingsRequest {

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

    @JsonProperty("eInvoiceEnabled")
    private Boolean eInvoiceEnabled;
    private Boolean offlineModeEnabled;
}
