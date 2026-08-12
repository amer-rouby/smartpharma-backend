package com.smartpharma.controller.settings;

import com.smartpharma.dto.settings.request.PharmacySettingsRequest;
import com.smartpharma.dto.settings.response.PharmacySettingsResponse;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.service.settings.PharmacySettingsService;
import com.smartpharma.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/pharmacy")
@RequiredArgsConstructor
@Slf4j
public class PharmacySettingsController {

    private final PharmacySettingsService pharmacySettingsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PharmacySettingsResponse>> getPharmacySettings(
            @RequestParam Long pharmacyId) {

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

        log.info("GET /api/settings/pharmacy - pharmacyId: {}", pharmacyId);

        PharmacySettingsResponse settings = pharmacySettingsService.getSettings(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings retrieved successfully"));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PharmacySettingsResponse>> updatePharmacySettings(
            @RequestParam Long pharmacyId,
            @Valid @RequestBody PharmacySettingsRequest request) {

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

        log.info("PUT /api/settings/pharmacy - pharmacyId: {}", pharmacyId);

        PharmacySettingsResponse settings = pharmacySettingsService.updateSettings(pharmacyId, request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings updated successfully"));
    }
}