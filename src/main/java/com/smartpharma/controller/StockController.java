// src/main/java/com/smartpharma/controller/StockController.java

package com.smartpharma.controller;

import com.smartpharma.dto.request.StockAdjustmentRequest;
import com.smartpharma.dto.request.StockBatchRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.StockBatchResponse;
import com.smartpharma.security.JwtService;  // ✅ أضف الـ import ده
import com.smartpharma.service.StockBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class StockController {

    private final StockBatchService stockBatchService;
    private final JwtService jwtService;  // ✅ أضف الـ dependency ده

    // ================================
    // ✅ GET all batches
    // ================================
    @GetMapping("/batches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockBatchResponse>>> getAllBatches(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long pharmacyId = extractPharmacyId(authHeader);
        Long userId = extractUserId(userDetails);

        log.info("Getting batches for pharmacy: {}, user: {}", pharmacyId, userId);

        List<StockBatchResponse> batches = stockBatchService.getAllBatches(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(batches));
    }

    // ================================
    // ✅ GET single batch
    // ================================
    @GetMapping("/batches/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockBatchResponse>> getBatch(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long pharmacyId = extractPharmacyId(authHeader);
        Long userId = extractUserId(userDetails);

        log.info("Getting batch {} for pharmacy: {}, user: {}", id, pharmacyId, userId);

        StockBatchResponse batch = stockBatchService.getBatch(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(batch));
    }

    // ================================
    // ✅ POST create new batch - FIXED: No @RequestParam pharmacyId
    // ================================
    @PostMapping("/batches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockBatchResponse>> createBatch(
            @Valid @RequestBody StockBatchRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {  // ✅ نأخذ الـ pharmacyId من الـ token

        // ✅ FIXED: Extract pharmacyId and userId from authenticated request
        Long pharmacyId = extractPharmacyId(authHeader);
        Long userId = extractUserId(userDetails);

        if (pharmacyId == null || userId == null) {
            log.warn("Could not extract pharmacyId or userId from token");
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: Invalid token"));
        }

        log.info("Creating batch for pharmacy: {}, user: {}, product: {}",
                pharmacyId, userId, request.getProductId());

        StockBatchResponse batch = stockBatchService.createBatch(request, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Batch created successfully"));
    }

    // ================================
    // ✅ PUT update batch
    // ================================
    @PutMapping("/batches/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockBatchResponse>> updateBatch(
            @PathVariable Long id,
            @Valid @RequestBody StockBatchRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long pharmacyId = extractPharmacyId(authHeader);
        Long userId = extractUserId(userDetails);

        log.info("Updating batch {} for pharmacy: {}, user: {}", id, pharmacyId, userId);

        StockBatchResponse batch = stockBatchService.updateBatch(id, request, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Batch updated successfully"));
    }

    // ================================
    // ✅ DELETE batch
    // ================================
    @DeleteMapping("/batches/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long pharmacyId = extractPharmacyId(authHeader);
        Long userId = extractUserId(userDetails);

        log.info("Deleting batch {} for pharmacy: {}, user: {}", id, pharmacyId, userId);

        stockBatchService.deleteBatch(id, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Batch deleted successfully"));
    }

    // ================================
    // ✅ GET expiring batches
    // ================================
    @GetMapping("/expiring")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockBatchResponse>>> getExpiringBatches(
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long pharmacyId = extractPharmacyId(authHeader);
        Long userId = extractUserId(userDetails);

        log.info("Getting expiring batches for pharmacy: {}, days: {}, user: {}", pharmacyId, days, userId);

        List<StockBatchResponse> batches = stockBatchService.getExpiringBatches(pharmacyId, days);
        return ResponseEntity.ok(ApiResponse.success(batches));
    }

    // ================================
    // ✅ GET expired batches
    // ================================
    @GetMapping("/expired")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockBatchResponse>>> getExpiredBatches(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long pharmacyId = extractPharmacyId(authHeader);
        Long userId = extractUserId(userDetails);

        log.info("Getting expired batches for pharmacy: {}, user: {}", pharmacyId, userId);

        List<StockBatchResponse> batches = stockBatchService.getExpiredBatches(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(batches));
    }

    // ================================
    // ✅ POST adjust stock
    // ================================
    @PostMapping("/batches/{id}/adjust")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockBatchResponse>> adjustStock(
            @PathVariable Long id,
            @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long pharmacyId = extractPharmacyId(authHeader);
        Long userId = extractUserId(userDetails);

        log.info("Adjusting stock for batch: {}, pharmacy: {}, user: {}", id, pharmacyId, userId);

        StockBatchResponse batch = stockBatchService.adjustStock(id, request, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Stock adjusted successfully"));
    }

    // ================================
    // ✅ Helpers: Extract userId and pharmacyId from authenticated request
    // ================================

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) return null;

        if (userDetails instanceof com.smartpharma.entity.User user) {
            return user.getId();
        }

        try {
            return Long.valueOf(userDetails.getUsername());
        } catch (NumberFormatException e) {
            log.warn("Could not parse userId from username: {}", userDetails.getUsername());
            return null;
        }
    }

    private Long extractPharmacyId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        try {
            String jwt = authHeader.substring(7);
            return jwtService.extractPharmacyId(jwt);  // ← ده اللي هيحل المشكلة!
        } catch (Exception e) {
            log.warn("Could not extract pharmacyId from token", e);
            return null;
        }
    }
}