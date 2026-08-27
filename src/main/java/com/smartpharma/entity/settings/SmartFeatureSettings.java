package com.smartpharma.entity.settings;

import com.smartpharma.entity.Pharmacy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// Per-pharmacy visibility switches for the smart/AI feature set. Every gated
// endpoint re-checks the relevant flag server-side (fails closed) in addition
// to the existing role checks, so this isn't just cosmetic. eInvoiceEnabled and
// offlineModeEnabled default to false since those need real external setup
// (ETA credentials, service worker rollout) before they do anything useful;
// everything else defaults to on.
@Entity
@Table(name = "smart_feature_settings", schema = "smartpharma")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartFeatureSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false, unique = true)
    private Pharmacy pharmacy;

    @Builder.Default
    private Boolean stockPredictionEnabled = true;

    @Builder.Default
    private Boolean reorderRecommendationsEnabled = true;

    @Builder.Default
    private Boolean pricingRecommendationsEnabled = true;

    @Builder.Default
    private Boolean supplierRecommendationsEnabled = true;

    @Builder.Default
    private Boolean dashboardInsightsEnabled = true;

    @Builder.Default
    private Boolean dailyBriefEnabled = true;

    @Builder.Default
    private Boolean anomalyDetectionEnabled = true;

    @Builder.Default
    private Boolean realtimeUpdatesEnabled = true;

    @Builder.Default
    private Boolean voiceSearchEnabled = true;

    @Builder.Default
    private Boolean aiAssistantEnabled = true;

    @Builder.Default
    private Boolean eInvoiceEnabled = false;

    @Builder.Default
    private Boolean offlineModeEnabled = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
