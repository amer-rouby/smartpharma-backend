package com.smartpharma.controller;

import com.smartpharma.dto.request.SaleRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.SaleTransactionDTO;
import com.smartpharma.dto.response.SalesReportResponse;
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
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class SalesController {

    private final SaleTransactionService saleTransactionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<SaleTransactionDTO>>> getAllSales(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("GET /api/sales - pharmacyId: {}, page: {}, size: {}, sortBy: {}, sortDirection: {}",
                pharmacyId, page, size, sortBy, sortDirection);

        Page<SaleTransactionDTO> sales = saleTransactionService.getAllSales(
                pharmacyId, page, size, sortBy, sortDirection);

        log.info("Returning {} sales (totalElements: {}, totalPages: {})",
                sales.getContent().size(), sales.getTotalElements(), sales.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(sales, "Sales retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<SaleTransactionDTO>> getSale(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {
        log.info("GET /api/sales/{} - pharmacyId: {}", id, pharmacyId);
        SaleTransactionDTO sale = saleTransactionService.getSaleById(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(sale, "Sale retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<SaleTransactionDTO>> createSale(
            @Valid @RequestBody SaleRequest request,
            @RequestParam Long pharmacyId,
            Authentication authentication) {
        log.info("POST /api/sales - pharmacyId: {}, items: {}", pharmacyId, request.getItems().size());

        Long currentUserId = null;
        if (authentication != null && authentication.getDetails() instanceof Map details) {
            currentUserId = (Long) details.get("userId");
        }

        SaleTransactionDTO response = saleTransactionService.createSale(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Sale created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<SaleTransactionDTO>> updateSale(
            @PathVariable Long id,
            @Valid @RequestBody SaleRequest request,
            @RequestParam Long pharmacyId) {
        log.info("PUT /api/sales/{} - pharmacyId: {}", id, pharmacyId);
        SaleTransactionDTO response = saleTransactionService.updateSale(id, request, pharmacyId);
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

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesAnalytics(
            @RequestParam Long pharmacyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "monthly") String period) {
        log.info("GET /api/sales/analytics - pharmacyId: {}, startDate: {}, endDate: {}, period: {}",
                pharmacyId, startDate, endDate, period);

        SalesReportResponse analytics = saleTransactionService.getSalesAnalytics(pharmacyId, startDate, endDate, period);
        return ResponseEntity.ok(ApiResponse.success(analytics, "Analytics retrieved successfully"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesStats(@RequestParam Long pharmacyId) {
        log.info("GET /api/sales/stats - pharmacyId: {}", pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getSalesStats(pharmacyId),
                "Sales stats retrieved successfully"));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodaySales(@RequestParam Long pharmacyId) {
        log.info("GET /api/sales/today - pharmacyId: {}", pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getTodaySales(pharmacyId),
                "Today sales retrieved successfully"));
    }

    @GetMapping("/today/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodaySalesSummary(@RequestParam Long pharmacyId) {
        log.info("GET /api/sales/today/summary - pharmacyId: {}", pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getTodaySalesSummary(pharmacyId),
                "Today sales summary retrieved successfully"));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<SaleTransactionDTO>>> getSalesByDateRange(
            @RequestParam Long pharmacyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/sales/range - pharmacyId: {}, startDate: {}, endDate: {}",
                pharmacyId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getSalesByDateRange(pharmacyId, startDate, endDate),
                "Sales by date range retrieved successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<SaleTransactionDTO>>> searchSales(
            @RequestParam Long pharmacyId,
            @RequestParam String query) {
        log.info("GET /api/sales/search - pharmacyId: {}, query: {}", pharmacyId, query);

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.searchSales(pharmacyId, query),
                "Search completed successfully"));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<SaleTransactionDTO>>> getRecentSales(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /api/sales/recent - pharmacyId: {}, limit: {}", pharmacyId, limit);

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getRecentSales(pharmacyId, limit),
                "Recent sales retrieved successfully"));
    }

    @GetMapping("/by-category")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesByCategory(
            @RequestParam Long pharmacyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/sales/by-category - pharmacyId: {}, startDate: {}, endDate: {}",
                pharmacyId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getSalesByCategory(pharmacyId, startDate, endDate),
                "Sales by category retrieved successfully"));
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopProducts(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /api/sales/top-products - pharmacyId: {}, limit: {}", pharmacyId, limit);

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getTopProducts(pharmacyId, limit),
                "Top products retrieved successfully"));
    }
}