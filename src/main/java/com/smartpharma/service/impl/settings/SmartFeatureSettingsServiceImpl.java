package com.smartpharma.service.impl.settings;

import com.smartpharma.dto.settings.request.SmartFeatureSettingsRequest;
import com.smartpharma.dto.settings.response.SmartFeatureSettingsResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.settings.SmartFeatureSettings;
import com.smartpharma.exception.ResourceNotFoundException;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.settings.SmartFeatureSettingsRepository;
import com.smartpharma.service.settings.SmartFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartFeatureSettingsServiceImpl implements SmartFeatureSettingsService {

    private final SmartFeatureSettingsRepository settingsRepository;
    private final PharmacyRepository pharmacyRepository;

    @Override
    @Transactional(readOnly = true)
    public SmartFeatureSettingsResponse getSettings(Long pharmacyId) {
        return SmartFeatureSettingsResponse.fromEntity(getOrCreate(pharmacyId));
    }

    @Override
    @Transactional
    public SmartFeatureSettingsResponse updateSettings(Long pharmacyId, SmartFeatureSettingsRequest request) {
        SmartFeatureSettings settings = getOrCreate(pharmacyId);

        if (request.getStockPredictionEnabled() != null) {
            settings.setStockPredictionEnabled(request.getStockPredictionEnabled());
        }
        if (request.getReorderRecommendationsEnabled() != null) {
            settings.setReorderRecommendationsEnabled(request.getReorderRecommendationsEnabled());
        }
        if (request.getPricingRecommendationsEnabled() != null) {
            settings.setPricingRecommendationsEnabled(request.getPricingRecommendationsEnabled());
        }
        if (request.getSupplierRecommendationsEnabled() != null) {
            settings.setSupplierRecommendationsEnabled(request.getSupplierRecommendationsEnabled());
        }
        if (request.getDashboardInsightsEnabled() != null) {
            settings.setDashboardInsightsEnabled(request.getDashboardInsightsEnabled());
        }
        if (request.getDailyBriefEnabled() != null) {
            settings.setDailyBriefEnabled(request.getDailyBriefEnabled());
        }
        if (request.getAnomalyDetectionEnabled() != null) {
            settings.setAnomalyDetectionEnabled(request.getAnomalyDetectionEnabled());
        }
        if (request.getRealtimeUpdatesEnabled() != null) {
            settings.setRealtimeUpdatesEnabled(request.getRealtimeUpdatesEnabled());
        }
        if (request.getVoiceSearchEnabled() != null) {
            settings.setVoiceSearchEnabled(request.getVoiceSearchEnabled());
        }
        if (request.getAiAssistantEnabled() != null) {
            settings.setAiAssistantEnabled(request.getAiAssistantEnabled());
        }
        if (request.getEInvoiceEnabled() != null) {
            settings.setEInvoiceEnabled(request.getEInvoiceEnabled());
        }
        if (request.getOfflineModeEnabled() != null) {
            settings.setOfflineModeEnabled(request.getOfflineModeEnabled());
        }

        SmartFeatureSettings saved = settingsRepository.save(settings);
        log.info("Smart feature settings updated for pharmacyId: {}", pharmacyId);
        return SmartFeatureSettingsResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public SmartFeatureSettings getOrCreate(Long pharmacyId) {
        return settingsRepository.findByPharmacyId(pharmacyId)
                .orElseGet(() -> createDefaultSettings(pharmacyId));
    }

    private SmartFeatureSettings createDefaultSettings(Long pharmacyId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("PHARMACY_NOT_FOUND", "Pharmacy not found"));

        SmartFeatureSettings defaults = SmartFeatureSettings.builder()
                .pharmacy(pharmacy)
                .build();
        return settingsRepository.save(defaults);
    }
}
