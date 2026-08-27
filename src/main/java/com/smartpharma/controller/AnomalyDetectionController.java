package com.smartpharma.controller;

import com.smartpharma.dto.response.AnomalyDetectionResponse;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.entity.AnomalyDetection;
import com.smartpharma.service.AnomalyDetectionService;
import com.smartpharma.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// No try/catch around the feature-flag-gated read - a disabled flag throws
// LocalizedException, handled globally with the correct status/error code.
@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionController {

    private final AnomalyDetectionService anomalyDetectionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<AnomalyDetectionResponse>>> getAnomalies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        AnomalyDetection.Status statusEnum = status != null ? AnomalyDetection.Status.valueOf(status) : null;
        AnomalyDetection.Type typeEnum = type != null ? AnomalyDetection.Type.valueOf(type) : null;
        Page<AnomalyDetectionResponse> result = anomalyDetectionService.getAnomalies(pharmacyId, statusEnum, typeEnum, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/counts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCounts() {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        Map<String, Long> counts = Map.of(
                "NEW", anomalyDetectionService.countByStatus(pharmacyId, AnomalyDetection.Status.NEW),
                "REVIEWED", anomalyDetectionService.countByStatus(pharmacyId, AnomalyDetection.Status.REVIEWED),
                "DISMISSED", anomalyDetectionService.countByStatus(pharmacyId, AnomalyDetection.Status.DISMISSED)
        );
        return ResponseEntity.ok(ApiResponse.success(counts));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AnomalyDetectionResponse>> markReviewed(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        AnomalyDetectionResponse response = anomalyDetectionService.markReviewed(id, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Marked as reviewed"));
    }

    @PutMapping("/{id}/dismiss")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AnomalyDetectionResponse>> dismiss(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        AnomalyDetectionResponse response = anomalyDetectionService.dismiss(id, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Dismissed"));
    }

    @PostMapping("/detect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> triggerDetection() {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        anomalyDetectionService.runDetectionForPharmacy(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Anomaly detection run completed"));
    }
}
