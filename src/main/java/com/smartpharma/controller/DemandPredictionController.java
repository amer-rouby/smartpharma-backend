package com.smartpharma.controller;

import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.DemandPredictionResponse;
import com.smartpharma.entity.User;
import com.smartpharma.security.JwtService;
import com.smartpharma.service.DemandPredictionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for demand prediction endpoints.
 */
@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class DemandPredictionController {

    private final DemandPredictionService predictionService;
    private final JwtService jwtService;

    // POST: Generate predictions for pharmacy
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<Void>> generatePredictions(
            @RequestParam Long pharmacyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate forDate,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        try {
            Long userId = extractUserId(userDetails);
            if (userId == null) {
                log.error("Failed to extract userId from token");
                return ResponseEntity.status(401).body(ApiResponse.error("Invalid authentication"));
            }

            // Validate pharmacy access
            Long tokenPharmacyId = extractPharmacyIdFromRequest(request);
            if (tokenPharmacyId != null && !tokenPharmacyId.equals(pharmacyId)) {
                log.error("PharmacyId mismatch: request={}, token={}", pharmacyId, tokenPharmacyId);
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }

            LocalDate targetDate = (forDate != null) ? forDate : LocalDate.now().plusDays(1);
            log.info("Generating predictions for pharmacy: {}, user: {}, date: {}", pharmacyId, userId, targetDate);

            predictionService.generatePredictions(pharmacyId, targetDate);
            return ResponseEntity.ok(ApiResponse.success(null, "Predictions generated for " + targetDate));

        } catch (Exception e) {
            log.error("Error generating predictions", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate predictions: " + e.getMessage()));
        }
    }

    // GET: Fetch upcoming predictions
    @GetMapping("/upcoming")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DemandPredictionResponse>>> getUpcomingPredictions(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "7") int daysAhead,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        try {
            Long tokenPharmacyId = extractPharmacyIdFromRequest(request);
            if (tokenPharmacyId != null && !tokenPharmacyId.equals(pharmacyId)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }

            log.info("Getting upcoming predictions for pharmacy: {}, days: {}", pharmacyId, daysAhead);
            List<DemandPredictionResponse> predictions = predictionService.getUpcomingPredictions(pharmacyId, daysAhead);
            return ResponseEntity.ok(ApiResponse.success(predictions));

        } catch (Exception e) {
            log.error("Error getting upcoming predictions", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get predictions: " + e.getMessage()));
        }
    }

    // GET: Paginated predictions list
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<DemandPredictionResponse>>> getPredictions(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        try {
            Long tokenPharmacyId = extractPharmacyIdFromRequest(request);
            if (tokenPharmacyId != null && !tokenPharmacyId.equals(pharmacyId)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }

            log.info("Getting predictions for pharmacy: {}, page: {}, size: {}", pharmacyId, page, size);
            Page<DemandPredictionResponse> predictions = predictionService.getPredictions(pharmacyId, page, size);
            return ResponseEntity.ok(ApiResponse.success(predictions));

        } catch (Exception e) {
            log.error("Error getting predictions", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get predictions: " + e.getMessage()));
        }
    }

    // GET: Accuracy statistics
    @GetMapping("/accuracy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccuracyStats(
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        try {
            Long tokenPharmacyId = extractPharmacyIdFromRequest(request);
            if (tokenPharmacyId != null && !tokenPharmacyId.equals(pharmacyId)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }

            log.info("Getting accuracy stats for pharmacy: {}", pharmacyId);
            Map<String, Object> stats = predictionService.getAccuracyStats(pharmacyId);
            return ResponseEntity.ok(ApiResponse.success(stats));

        } catch (Exception e) {
            log.error("Error getting accuracy stats", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get stats: " + e.getMessage()));
        }
    }

    // PUT: Update prediction with actual sales
    @PutMapping("/{id}/actual")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<Void>> updateActualQuantity(
            @PathVariable Long id,
            @RequestParam Integer actualQuantity,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Long userId = extractUserId(userDetails);
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Invalid authentication"));
            }

            log.info("Updating actual quantity for prediction: {}, actual: {}, user: {}", id, actualQuantity, userId);
            predictionService.updatePredictionWithActual(id, actualQuantity);
            return ResponseEntity.ok(ApiResponse.success(null, "Actual quantity updated"));

        } catch (Exception e) {
            log.error("Error updating actual quantity", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update: " + e.getMessage()));
        }
    }

    // Extract user ID from authentication principal
    private Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        if (userDetails instanceof User user) return user.getId();
        try {
            return Long.valueOf(userDetails.getUsername());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Extract pharmacy ID from JWT token
    private Long extractPharmacyIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            String jwt = authHeader.substring(7);
            return jwtService.extractPharmacyId(jwt);
        } catch (Exception e) {
            log.warn("Failed to extract pharmacyId from token", e);
            return null;
        }
    }
}