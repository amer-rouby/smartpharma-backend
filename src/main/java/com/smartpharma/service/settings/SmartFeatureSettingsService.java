package com.smartpharma.service.settings;

import com.smartpharma.dto.settings.request.SmartFeatureSettingsRequest;
import com.smartpharma.dto.settings.response.SmartFeatureSettingsResponse;
import com.smartpharma.entity.settings.SmartFeatureSettings;

public interface SmartFeatureSettingsService {

    SmartFeatureSettingsResponse getSettings(Long pharmacyId);

    SmartFeatureSettingsResponse updateSettings(Long pharmacyId, SmartFeatureSettingsRequest request);

    // Used internally by feature-gating checks in other services/controllers -
    // returns the entity (creating defaults on first access) rather than the DTO.
    SmartFeatureSettings getOrCreate(Long pharmacyId);
}
