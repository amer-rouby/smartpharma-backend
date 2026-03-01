package com.smartpharma.controller;

import com.smartpharma.dto.request.SupplierRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.SupplierResponse;
import com.smartpharma.entity.User;
import com.smartpharma.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getAllSuppliers(
            @RequestParam Long pharmacyId) {
        log.info("GET /api/suppliers - pharmacyId: {}", pharmacyId);
        List<SupplierResponse> suppliers = supplierService.getAllSuppliers(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(suppliers));
    }

    @GetMapping("/paginated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<SupplierResponse>>> getSuppliersPaginated(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/suppliers/paginated - pharmacyId: {}, page: {}, size: {}", pharmacyId, page, size);
        Page<SupplierResponse> suppliers = supplierService.getSuppliersPaginated(pharmacyId, page, size);
        return ResponseEntity.ok(ApiResponse.success(suppliers));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SupplierResponse>> getSupplier(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {
        log.info("GET /api/suppliers/{} - pharmacyId: {}", id, pharmacyId);
        SupplierResponse supplier = supplierService.getSupplier(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(supplier));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(
            @Valid @RequestBody SupplierRequest request,
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        log.info("POST /api/suppliers - pharmacyId: {}, userId: {}", pharmacyId, userId);
        SupplierResponse supplier = supplierService.createSupplier(request, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(supplier, "Supplier created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request,
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        log.info("PUT /api/suppliers/{} - pharmacyId: {}, userId: {}", id, pharmacyId, userId);
        SupplierResponse supplier = supplierService.updateSupplier(id, request, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(supplier, "Supplier updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(
            @PathVariable Long id,
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        log.info("DELETE /api/suppliers/{} - pharmacyId: {}, userId: {}", id, pharmacyId, userId);
        supplierService.deleteSupplier(id, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Supplier deleted successfully"));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countSuppliers(@RequestParam Long pharmacyId) {
        Long count = supplierService.countSuppliers(pharmacyId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> searchSuppliers(
            @RequestParam Long pharmacyId,
            @RequestParam String query) {
        log.info("GET /api/suppliers/search - pharmacyId: {}, query: {}", pharmacyId, query);
        List<SupplierResponse> suppliers = supplierService.searchSuppliers(pharmacyId, query);
        return ResponseEntity.ok(ApiResponse.success(suppliers));
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
}