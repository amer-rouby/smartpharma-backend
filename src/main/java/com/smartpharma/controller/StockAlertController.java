package com.smartpharma.controller;

import com.smartpharma.dto.response.AlertStatsResponse;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.StockAlertResponse;
import com.smartpharma.entity.StockAlert;
import com.smartpharma.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class StockAlertController {

    private final StockAlertService alertService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<StockAlertResponse>>> getAlerts(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting alerts for pharmacy: {}, page: {}, size: {}", pharmacyId, page, size);

        Page<StockAlertResponse> alerts = alertService.getAlerts(pharmacyId, page, size);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AlertStatsResponse>> getAlertStats(
            @RequestParam Long pharmacyId) {

        log.info("Getting alert stats for pharmacy: {}", pharmacyId);

        AlertStatsResponse stats = alertService.getAlertStats(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockAlertResponse>>> getActiveAlerts(
            @RequestParam Long pharmacyId) {

        log.info("Getting active alerts for pharmacy: {}", pharmacyId);

        List<StockAlertResponse> alerts = alertService.getActiveAlerts(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        log.info("Marking alert {} as read by user {}", id, userId);

        alertService.markAsRead(id, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Alert marked as read"));
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        log.info("Marking all alerts as read for pharmacy {} by user {}", pharmacyId, userId);

        alertService.markAllAsRead(pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "All alerts marked as read"));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> resolveAlert(
            @PathVariable Long id,
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        log.info("Resolving alert {} by user {}", id, userId);

        alertService.resolveAlert(id, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Alert resolved"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteAlert(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        log.info("Deleting alert {} for pharmacy {}", id, pharmacyId);

        alertService.deleteAlert(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Alert deleted"));
    }

    @PostMapping("/generate/low-stock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> generateLowStockAlerts(
            @RequestParam Long pharmacyId) {

        log.info("Generating low stock alerts for pharmacy {}", pharmacyId);

        alertService.generateLowStockAlerts(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Low stock alerts generated"));
    }

    @PostMapping("/generate/expiry")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> generateExpiryAlerts(
            @RequestParam Long pharmacyId) {

        log.info("Generating expiry alerts for pharmacy {}", pharmacyId);

        alertService.generateExpiryAlerts(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Expiry alerts generated"));
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        if (userDetails instanceof com.smartpharma.entity.User user) return user.getId();
        try {
            return Long.valueOf(userDetails.getUsername());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}