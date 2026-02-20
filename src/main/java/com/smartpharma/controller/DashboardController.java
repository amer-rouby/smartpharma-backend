package com.smartpharma.controller;

import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(
            @RequestParam Long pharmacyId) {

        log.info("GET /api/dashboard/stats - pharmacyId: {}", pharmacyId);

        try {
            Map<String, Object> stats = dashboardService.getDashboardStats(pharmacyId);
            return ResponseEntity.ok(ApiResponse.success(stats, "Dashboard stats retrieved"));
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}