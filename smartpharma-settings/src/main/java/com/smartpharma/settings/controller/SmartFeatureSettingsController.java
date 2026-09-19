package com.smartpharma.settings.controller;

import com.smartpharma.common.dto.ApiResponse;
import com.smartpharma.settings.dto.request.SmartFeatureSettingsRequest;
import com.smartpharma.settings.dto.response.SmartFeatureSettingsResponse;
import com.smartpharma.settings.service.SmartFeatureSettingsService;
import com.smartpharma.common.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/smart-features")
@RequiredArgsConstructor
@Slf4j
public class SmartFeatureSettingsController {

    private final SmartFeatureSettingsService smartFeatureSettingsService;

    // Any authenticated role can read the flags - screens/nav entries all over
    // the app need to know what's enabled, not just the settings screen itself.
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SmartFeatureSettingsResponse>> getSettings(
            @RequestParam Long pharmacyId) {

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

        log.info("GET /api/settings/smart-features - pharmacyId: {}", pharmacyId);

        SmartFeatureSettingsResponse settings = smartFeatureSettingsService.getSettings(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings retrieved successfully"));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SmartFeatureSettingsResponse>> updateSettings(
            @RequestParam Long pharmacyId,
            @Valid @RequestBody SmartFeatureSettingsRequest request) {

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

        log.info("PUT /api/settings/smart-features - pharmacyId: {}", pharmacyId);

        SmartFeatureSettingsResponse settings = smartFeatureSettingsService.updateSettings(pharmacyId, request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings updated successfully"));
    }
}
