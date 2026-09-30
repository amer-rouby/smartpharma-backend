package com.smartpharma.einvoice.controller;

import com.smartpharma.common.dto.ApiResponse;
import com.smartpharma.common.util.SecurityUtils;
import com.smartpharma.einvoice.dto.request.EtaPosDeviceRequest;
import com.smartpharma.einvoice.dto.request.EtaSettingsRequest;
import com.smartpharma.einvoice.dto.response.EtaPosDeviceResponse;
import com.smartpharma.einvoice.dto.response.EtaSettingsResponse;
import com.smartpharma.einvoice.service.EtaSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ETA taxpayer data, API credentials and POS devices - ADMIN only, since the
// secrets here let anyone issue receipts in the pharmacy's name.
@RestController
@RequestMapping("/api/e-invoice/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EtaSettingsController {

    private final EtaSettingsService settingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<EtaSettingsResponse>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(settingsService.getSettings(SecurityUtils.getCurrentPharmacyId())));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<EtaSettingsResponse>> saveSettings(@Valid @RequestBody EtaSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                settingsService.saveSettings(SecurityUtils.getCurrentPharmacyId(), request)));
    }

    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<List<EtaPosDeviceResponse>>> getDevices() {
        return ResponseEntity.ok(ApiResponse.success(settingsService.getDevices(SecurityUtils.getCurrentPharmacyId())));
    }

    @PostMapping("/devices")
    public ResponseEntity<ApiResponse<EtaPosDeviceResponse>> createDevice(@Valid @RequestBody EtaPosDeviceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                settingsService.createDevice(SecurityUtils.getCurrentPharmacyId(), request)));
    }

    @PutMapping("/devices/{deviceId}")
    public ResponseEntity<ApiResponse<EtaPosDeviceResponse>> updateDevice(@PathVariable Long deviceId,
                                                                          @Valid @RequestBody EtaPosDeviceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                settingsService.updateDevice(SecurityUtils.getCurrentPharmacyId(), deviceId, request)));
    }

    @PostMapping("/devices/{deviceId}/test")
    public ResponseEntity<ApiResponse<String>> testConnection(@PathVariable Long deviceId) {
        return ResponseEntity.ok(ApiResponse.success(
                settingsService.testConnection(SecurityUtils.getCurrentPharmacyId(), deviceId)));
    }
}
