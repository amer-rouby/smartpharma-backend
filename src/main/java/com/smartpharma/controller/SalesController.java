package com.smartpharma.controller;

import com.smartpharma.dto.request.SaleRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.SaleTransactionResponse;
import com.smartpharma.entity.SaleTransaction;
import com.smartpharma.service.SaleTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class SalesController {

    private final SaleTransactionService saleTransactionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<SaleTransaction>>> getAllSales(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/sales - pharmacyId: {}, page: {}, size: {}", pharmacyId, page, size);

        Page<SaleTransaction> sales = saleTransactionService.getAllSales(pharmacyId, page, size);
        return ResponseEntity.ok(ApiResponse.success(sales, "Sales retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<SaleTransaction>> getSale(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        log.info("GET /api/sales/{} - pharmacyId: {}", id, pharmacyId);

        SaleTransaction sale = saleTransactionService.getSaleById(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(sale, "Sale retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<SaleTransactionResponse>> createSale(
            @Valid @RequestBody SaleRequest request,
            @RequestParam Long pharmacyId,
            Authentication authentication) {

        log.info("POST /api/sales - pharmacyId: {}, items: {}", pharmacyId, request.getItems().size());

        Long currentUserId = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            // Optional: extract user ID if needed for audit
            currentUserId = 1L; // Placeholder - implement proper user ID extraction
        }

        SaleTransactionResponse response = saleTransactionService.createSale(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Sale created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<SaleTransactionResponse>> updateSale(
            @PathVariable Long id,
            @Valid @RequestBody SaleRequest request,
            @RequestParam Long pharmacyId) {

        log.info("PUT /api/sales/{} - pharmacyId: {}", id, pharmacyId);

        SaleTransactionResponse response = saleTransactionService.updateSale(id, request, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(response, "Sale updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSale(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        log.info("DELETE /api/sales/{} - pharmacyId: {}", id, pharmacyId);

        saleTransactionService.deleteSale(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Sale deleted successfully"));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodaySales(@RequestParam Long pharmacyId) {
        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getTodaySales(pharmacyId),
                "Today sales retrieved successfully"));
    }

    @GetMapping("/today/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodaySalesSummary(@RequestParam Long pharmacyId) {
        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getTodaySalesSummary(pharmacyId),
                "Today sales summary retrieved successfully"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesStats(@RequestParam Long pharmacyId) {
        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getSalesStats(pharmacyId),
                "Sales stats retrieved successfully"));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<SaleTransaction>>> getSalesByDateRange(
            @RequestParam Long pharmacyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getSalesByDateRange(pharmacyId, startDate, endDate),
                "Sales by date range retrieved successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<SaleTransaction>>> searchSales(
            @RequestParam Long pharmacyId,
            @RequestParam String query) {

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.searchSales(pharmacyId, query),
                "Search completed successfully"));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<SaleTransactionResponse>>> getRecentSales(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getRecentSales(pharmacyId, limit),
                "Recent sales retrieved successfully"));
    }
}