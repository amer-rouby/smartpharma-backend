package com.smartpharma.service.settings;

import com.smartpharma.dto.settings.request.PharmacySettingsRequest;
import com.smartpharma.dto.settings.response.PharmacySettingsResponse;

public interface PharmacySettingsService {

    PharmacySettingsResponse getSettings(Long pharmacyId);

    PharmacySettingsResponse updateSettings(Long pharmacyId, PharmacySettingsRequest request);
}