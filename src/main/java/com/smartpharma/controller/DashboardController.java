// src/main/java/com/smartpharma/controller/DashboardController.java

package com.smartpharma.controller;

import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.DashboardResponse;
import com.smartpharma.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardStats(
            @RequestParam Long pharmacyId) {

        log.info("GET /api/dashboard/stats - pharmacyId: {}", pharmacyId);

        DashboardResponse stats = dashboardService.getDashboardStats(pharmacyId);

        return ResponseEntity.ok(ApiResponse.success(
                stats,
                "Dashboard stats retrieved successfully"
        ));
    }
}