package com.smartpharma.settings.service;

import com.smartpharma.settings.dto.request.SmartFeatureSettingsRequest;
import com.smartpharma.settings.dto.response.SmartFeatureSettingsResponse;
import com.smartpharma.settings.entity.SmartFeatureSettings;

public interface SmartFeatureSettingsService {

    SmartFeatureSettingsResponse getSettings(Long pharmacyId);

    SmartFeatureSettingsResponse updateSettings(Long pharmacyId, SmartFeatureSettingsRequest request);

    // Used internally by feature-gating checks in other services/controllers -
    // returns the entity (creating defaults on first access) rather than the DTO.
    SmartFeatureSettings getOrCreate(Long pharmacyId);
}
