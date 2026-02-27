package com.smartpharma.controller;

import com.smartpharma.dto.request.StockMovementRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.StockMovementResponse;
import com.smartpharma.dto.response.StockMovementStats;
import com.smartpharma.service.StockMovementService;
import jakarta.validation.Valid;
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
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/stock/movements")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class StockMovementController {

    private final StockMovementService movementService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockMovementResponse>> createMovement(
            @Valid @RequestBody StockMovementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        log.info("Creating stock movement for user: {}", userId);

        StockMovementResponse movement = movementService.createMovement(request, userId);
        return ResponseEntity.ok(ApiResponse.success(movement, "Movement created successfully"));
    }

    @GetMapping("/pharmacy/{pharmacyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<StockMovementResponse>>> getMovementsByPharmacy(
            @PathVariable Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting movements for pharmacy: {}", pharmacyId);

        Page<StockMovementResponse> movements = movementService.getMovementsByPharmacy(pharmacyId, page, size);
        return ResponseEntity.ok(ApiResponse.success(movements));
    }

    @GetMapping("/batch/{batchId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<StockMovementResponse>>> getMovementsByBatch(
            @PathVariable Long batchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting movements for batch: {}", batchId);

        Page<StockMovementResponse> movements = movementService.getMovementsByBatch(batchId, page, size);
        return ResponseEntity.ok(ApiResponse.success(movements));
    }

    @GetMapping("/date-range")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<StockMovementResponse>>> getMovementsByDateRange(
            @RequestParam Long pharmacyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting movements for pharmacy: {} from {} to {}", pharmacyId, startDate, endDate);

        Page<StockMovementResponse> movements = movementService.getMovementsByDateRange(pharmacyId, startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success(movements));
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockMovementStats>> getMovementStats(
            @RequestParam Long pharmacyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting movement stats for pharmacy: {} from {} to {}", pharmacyId, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        StockMovementStats stats = movementService.getMovementStats(pharmacyId, startDateTime, endDateTime);
        return ResponseEntity.ok(ApiResponse.success(stats));
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