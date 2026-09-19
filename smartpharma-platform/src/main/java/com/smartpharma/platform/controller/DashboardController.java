// src/main/java/com/smartpharma/controller/DashboardController.java

package com.smartpharma.platform.controller;

import com.smartpharma.common.dto.ApiResponse;
import com.smartpharma.platform.dto.response.DashboardResponse;
import com.smartpharma.platform.dto.response.SmartInsightsDTO;
import com.smartpharma.platform.service.DashboardService;
import com.smartpharma.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardStats(
            @RequestParam Long pharmacyId) {

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

        log.info("GET /api/dashboard/stats - pharmacyId: {}", pharmacyId);

        DashboardResponse stats = dashboardService.getDashboardStats(pharmacyId);

        return ResponseEntity.ok(ApiResponse.success(
                stats,
                "Dashboard stats retrieved successfully"
        ));
    }

    // No try/catch - a disabled feature flag throws LocalizedException, handled
    // globally with the correct status and translatable error code.
    @GetMapping("/smart-insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<SmartInsightsDTO>> getSmartInsights(
            @RequestParam(required = false) Long pharmacyId) {
        Long resolvedPharmacyId = SecurityUtils.getCurrentPharmacyId();
        SmartInsightsDTO insights = dashboardService.getSmartInsights(resolvedPharmacyId);
        return ResponseEntity.ok(ApiResponse.success(insights, "Smart insights retrieved successfully"));
    }
}