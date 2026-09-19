package com.smartpharma.settings.service;

import com.smartpharma.settings.dto.request.PharmacySettingsRequest;
import com.smartpharma.settings.dto.response.PharmacySettingsResponse;

public interface PharmacySettingsService {

    PharmacySettingsResponse getSettings(Long pharmacyId);

    PharmacySettingsResponse updateSettings(Long pharmacyId, PharmacySettingsRequest request);
}