package com.smartpharma.controller;

import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.service.SalesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class SalesController {

    private final SalesService salesService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getAllSales(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                salesService.getAllSales(pharmacyId, page, size),
                "Sales retrieved successfully"
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getSale(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {
        return ResponseEntity.ok(ApiResponse.success(
                salesService.getSaleById(id, pharmacyId),
                "Sale retrieved successfully"
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<?>> createSale(
            @RequestBody Map<String, Object> saleRequest,
            @RequestParam Long pharmacyId) {
        return ResponseEntity.ok(ApiResponse.success(
                salesService.createSale(saleRequest, pharmacyId),
                "Sale created successfully"
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<?>> updateSale(
            @PathVariable Long id,
            @RequestBody Map<String, Object> saleRequest,
            @RequestParam Long pharmacyId) {
        return ResponseEntity.ok(ApiResponse.success(
                salesService.updateSale(id, saleRequest, pharmacyId),
                "Sale updated successfully"
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSale(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {
        salesService.deleteSale(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Sale deleted successfully"));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getTodaySales(
            @RequestParam Long pharmacyId) {
        return ResponseEntity.ok(ApiResponse.success(
                salesService.getTodaySales(pharmacyId),
                "Today sales retrieved successfully"
        ));
    }

    @GetMapping("/today/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getTodaySalesSummary(
            @RequestParam Long pharmacyId) {
        return ResponseEntity.ok(ApiResponse.success(
                salesService.getTodaySalesSummary(pharmacyId),
                "Today sales summary retrieved successfully"
        ));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getSalesStats(
            @RequestParam Long pharmacyId) {
        return ResponseEntity.ok(ApiResponse.success(
                salesService.getSalesStats(pharmacyId),
                "Sales stats retrieved successfully"
        ));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getSalesByDateRange(
            @RequestParam Long pharmacyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                salesService.getSalesByDateRange(pharmacyId, startDate, endDate),
                "Sales by date range retrieved successfully"
        ));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> searchSales(
            @RequestParam Long pharmacyId,
            @RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.success(
                salesService.searchSales(pharmacyId, query),
                "Search completed successfully"
        ));
    }
}