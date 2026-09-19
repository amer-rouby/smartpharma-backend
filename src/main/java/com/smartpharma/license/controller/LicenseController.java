package com.smartpharma.license.controller;

import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.license.dto.request.LicenseGenerateRequest;
import com.smartpharma.license.dto.request.LicenseRenewRequest;
import com.smartpharma.license.dto.response.LicenseGenerateResponse;
import com.smartpharma.license.dto.response.LicenseStatusResponse;
import com.smartpharma.license.service.LicenseService;
import com.smartpharma.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/license")
@RequiredArgsConstructor
@Slf4j
public class LicenseController {

    private final LicenseService licenseService;

    // Any authenticated role can check status, not just admin.
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LicenseStatusResponse>> getStatus() {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        return ResponseEntity.ok(ApiResponse.success(licenseService.getStatus(pharmacyId)));
    }

    // ADMIN-only, same convention as other pharmacy-wide settings writes.
    @PostMapping("/renew")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LicenseStatusResponse>> renew(@Valid @RequestBody LicenseRenewRequest request) {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        log.info("POST /api/license/renew - pharmacyId: {}", pharmacyId);
        LicenseStatusResponse status = licenseService.renew(pharmacyId, request.getCode());
        return ResponseEntity.ok(ApiResponse.success(status, "Subscription renewed successfully"));
    }

    // Vendor-only: fails closed unless license.private-key-path is set.
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LicenseGenerateResponse>> generate(@Valid @RequestBody LicenseGenerateRequest request) {
        log.info("POST /api/license/generate - pharmacyId: {}", request.getPharmacyId());
        LicenseGenerateResponse response = licenseService.generateCode(request.getPharmacyId(), request.getMonths());
        return ResponseEntity.ok(ApiResponse.success(response, "Activation code generated"));
    }
}
