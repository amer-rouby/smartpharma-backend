// src/main/java/com/smartpharma/controller/StockController.java

package com.smartpharma.controller;

import com.smartpharma.dto.request.StockAdjustmentRequest;
import com.smartpharma.dto.request.StockBatchRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.StockBatchResponse;
import com.smartpharma.entity.User;
import com.smartpharma.security.JwtService;
import com.smartpharma.service.StockBatchService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class StockController {

    private final StockBatchService stockBatchService;
    private final JwtService jwtService;

    @GetMapping("/batches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<StockBatchResponse>>> getAllBatches(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        log.info("Getting batches for pharmacy: {}, page: {}, size: {}, user: {}", pharmacyId, page, size, userId);

        Page<StockBatchResponse> batches = stockBatchService.getAllBatches(pharmacyId, page, size);
        return ResponseEntity.ok(ApiResponse.success(batches));
    }

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

    @PostMapping("/batches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockBatchResponse>> createBatch(
            @Valid @RequestBody StockBatchRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long pharmacyId = extractPharmacyId(authHeader);
        Long userId = extractUserId(userDetails);

        if (pharmacyId == null || userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: Invalid token"));
        }

        log.info("Creating batch for pharmacy: {}, user: {}, product: {}",
                pharmacyId, userId, request.getProductId());

        StockBatchResponse batch = stockBatchService.createBatch(request, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Batch created successfully"));
    }

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

    @GetMapping("/expiring")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockBatchResponse>>> getExpiringBatches(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        log.info("Getting expiring batches for pharmacy: {}, days: {}, user: {}", pharmacyId, days, userId);

        List<StockBatchResponse> batches = stockBatchService.getExpiringBatches(pharmacyId, days);
        return ResponseEntity.ok(ApiResponse.success(batches));
    }

    @GetMapping("/expired")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockBatchResponse>>> getExpiredBatches(
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        log.info("Getting expired batches for pharmacy: {}, user: {}", pharmacyId, userId);

        List<StockBatchResponse> batches = stockBatchService.getExpiredBatches(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(batches));
    }

    @PostMapping("/batches/{id}/adjust")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockBatchResponse>> adjustStock(
            @PathVariable Long id,
            @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        log.info("Adjusting stock for batch: {}, user: {}", id, userId);

        StockBatchResponse batch = stockBatchService.adjustStock(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Stock adjusted successfully"));
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        if (userDetails instanceof User user) return user.getId();
        try {
            return Long.valueOf(userDetails.getUsername());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long extractPharmacyId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            String jwt = authHeader.substring(7);
            return jwtService.extractPharmacyId(jwt);
        } catch (Exception e) {
            return null;
        }
    }
}