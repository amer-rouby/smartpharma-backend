package com.smartpharma.controller;

import com.smartpharma.dto.request.ProductRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.ProductResponse;
import com.smartpharma.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    private final ProductService productService;

    /**
     * Retrieve all products for a specific pharmacy.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @RequestParam Long pharmacyId) {

        List<ProductResponse> products = productService.getAllProducts(pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(products, "Products retrieved successfully")
        );
    }

    /**
     * Retrieve a single product by its ID within a specific pharmacy.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        ProductResponse product = productService.getProduct(id, pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(product, "Product retrieved successfully")
        );
    }

    /**
     * Create a new product for a specific pharmacy.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            @RequestParam Long pharmacyId) {

        ProductResponse product = productService.createProduct(request, pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(product, "Product created successfully")
        );
    }

    /**
     * Update an existing product by ID within a specific pharmacy.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            @RequestParam Long pharmacyId) {

        ProductResponse product = productService.updateProduct(id, request, pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(product, "Product updated successfully")
        );
    }

    /**
     * Delete a product by ID from a specific pharmacy.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        productService.deleteProduct(id, pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Product deleted successfully")
        );
    }

    /**
     * Search for products within a specific pharmacy using a query string.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam Long pharmacyId,
            @RequestParam String query) {

        List<ProductResponse> products = productService.searchProducts(pharmacyId, query);
        return ResponseEntity.ok(
                ApiResponse.success(products, "Search completed successfully")
        );
    }

    /**
     * Retrieve products that are low in stock for a specific pharmacy.
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStockProducts(
            @RequestParam Long pharmacyId) {

        List<ProductResponse> products = productService.getLowStockProducts(pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(products, "Low stock products retrieved")
        );
    }
}
