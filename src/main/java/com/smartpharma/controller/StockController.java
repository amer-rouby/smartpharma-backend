package com.smartpharma.controller;

import com.smartpharma.dto.request.StockAdjustmentRequest;
import com.smartpharma.dto.request.StockBatchRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.StockAdjustmentHistoryDTO;
import com.smartpharma.dto.response.StockBatchResponse;
import com.smartpharma.security.JwtService;
import com.smartpharma.service.StockBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class StockController {

    private final StockBatchService stockBatchService;
    private final JwtService jwtService;

    @GetMapping("/batches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<StockBatchResponse>>> getAllBatches(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/stock/batches - pharmacyId: {}, page: {}, size: {}", pharmacyId, page, size);

        Page<StockBatchResponse> batches = stockBatchService.getAllBatches(pharmacyId, page, size);
        return ResponseEntity.ok(ApiResponse.success(batches, "Stock batches retrieved successfully"));
    }

    @GetMapping("/batches/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockBatchResponse>> getBatch(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        log.info("GET /api/stock/batches/{} - pharmacyId: {}", id, pharmacyId);

        StockBatchResponse batch = stockBatchService.getBatch(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Stock batch retrieved successfully"));
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<StockBatchResponse>> createBatch(
            @Valid @RequestBody StockBatchRequest request,
            @RequestParam Long pharmacyId,
            Authentication authentication) {

        log.info("POST /api/stock/batches - pharmacyId: {}, product: {}",
                pharmacyId, request.getProductId());

        Long userId = extractUserId(authentication);
        StockBatchResponse batch = stockBatchService.createBatch(request, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Batch created successfully"));
    }

    @PutMapping("/batches/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<StockBatchResponse>> updateBatch(
            @PathVariable Long id,
            @Valid @RequestBody StockBatchRequest request,
            @RequestParam Long pharmacyId,
            Authentication authentication) {

        log.info("PUT /api/stock/batches/{} - pharmacyId: {}", id, pharmacyId);

        Long userId = extractUserId(authentication);
        StockBatchResponse batch = stockBatchService.updateBatch(id, request, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Batch updated successfully"));
    }

    @DeleteMapping("/batches/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(
            @PathVariable Long id,
            @RequestParam Long pharmacyId,
            Authentication authentication) {

        log.info("DELETE /api/stock/batches/{} - pharmacyId: {}", id, pharmacyId);

        Long userId = extractUserId(authentication);
        stockBatchService.deleteBatch(id, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Batch deleted successfully"));
    }

    @GetMapping("/expiring")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockBatchResponse>>> getExpiringBatches(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "30") int days) {

        log.info("GET /api/stock/expiring - pharmacyId: {}, days: {}", pharmacyId, days);

        List<StockBatchResponse> batches = stockBatchService.getExpiringBatches(pharmacyId, days);
        return ResponseEntity.ok(ApiResponse.success(batches, "Expiring batches retrieved successfully"));
    }

    @GetMapping("/expired")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockBatchResponse>>> getExpiredBatches(
            @RequestParam Long pharmacyId) {

        log.info("GET /api/stock/expired - pharmacyId: {}", pharmacyId);

        List<StockBatchResponse> batches = stockBatchService.getExpiredBatches(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(batches, "Expired batches retrieved successfully"));
    }

    @PostMapping("/batches/{id}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<StockBatchResponse>> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequest request,
            Authentication authentication) {

        log.info("POST /api/stock/batches/{}/adjust - type: {}, quantity: {}",
                id, request.getType(), request.getQuantity());

        Long userId = extractUserId(authentication);
        StockBatchResponse batch = stockBatchService.adjustStock(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Stock adjusted successfully"));
    }

    @GetMapping("/batches/{id}/adjustments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockAdjustmentHistoryDTO>>> getAdjustmentHistory(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        log.info("GET /api/stock/batches/{}/adjustments - pharmacyId: {}", id, pharmacyId);

        List<StockAdjustmentHistoryDTO> history = stockBatchService.getAdjustmentHistory(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(history, "Adjustment history retrieved successfully"));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) return null;

        Object details = authentication.getDetails();
        if (details instanceof Map) {
            Map<?, ?> detailsMap = (Map<?, ?>) details;
            Object userIdObj = detailsMap.get("userId");
            if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            }
            if (userIdObj instanceof Integer) {
                return ((Integer) userIdObj).longValue();
            }
            if (userIdObj instanceof String) {
                try {
                    return Long.valueOf((String) userIdObj);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        if (authentication.getPrincipal() instanceof com.smartpharma.entity.User user) {
            return user.getId();
        }

        return null;
    }
}