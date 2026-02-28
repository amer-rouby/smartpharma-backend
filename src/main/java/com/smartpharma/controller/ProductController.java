package com.smartpharma.controller;

import com.smartpharma.dto.request.ProductRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.ProductResponse;
import com.smartpharma.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing products within the SmartPharma system.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @RequestParam Long pharmacyId) {

        log.info("GET /api/products - pharmacyId: {}", pharmacyId);

        List<ProductResponse> products = productService.getAllProducts(pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(products, "Products retrieved successfully")
        );
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getProductsCount(
            @RequestParam Long pharmacyId) {

        log.info("GET /api/products/count - pharmacyId: {}", pharmacyId);

        Long count = productService.getProductsCount(pharmacyId);

        Map<String, Long> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Products count retrieved successfully")
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        log.info("GET /api/products/{} - pharmacyId: {}", id, pharmacyId);

        ProductResponse product = productService.getProduct(id, pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(product, "Product retrieved successfully")
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            @RequestParam Long pharmacyId) {

        log.info("POST /api/products - pharmacyId: {}, productName: {}",
                pharmacyId, request.getName());

        ProductResponse product = productService.createProduct(request, pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(product, "Product created successfully")
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            @RequestParam Long pharmacyId) {

        log.info("PUT /api/products/{} - pharmacyId: {}", id, pharmacyId);

        ProductResponse product = productService.updateProduct(id, request, pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(product, "Product updated successfully")
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        log.info("DELETE /api/products/{} - pharmacyId: {}", id, pharmacyId);

        productService.deleteProduct(id, pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Product deleted successfully")
        );
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam Long pharmacyId,
            @RequestParam String query) {

        log.info("GET /api/products/search - pharmacyId: {}, query: '{}'", pharmacyId, query);

        List<ProductResponse> products = productService.searchProducts(pharmacyId, query);
        return ResponseEntity.ok(
                ApiResponse.success(products, "Search completed successfully")
        );
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStockProducts(
            @RequestParam Long pharmacyId) {

        log.info("GET /api/products/low-stock - pharmacyId: {}", pharmacyId);

        List<ProductResponse> products = productService.getLowStockProducts(pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(products, "Low stock products retrieved")
        );
    }
}