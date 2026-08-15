package com.smartpharma.controller;

import com.smartpharma.dto.request.ProductRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.ProductResponse;
import com.smartpharma.service.ProductService;
import com.smartpharma.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @RequestParam Long pharmacyId) {

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

        log.info("GET /api/products - pharmacyId: {}", pharmacyId);

        List<ProductResponse> products = productService.getAllProducts(pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(products, "Products retrieved successfully")
        );
    }

    /**
     * Real backend-paginated + searchable product listing for the Products management
     * screen. Deliberately a separate endpoint from GET /api/products (above), which
     * intentionally still returns the full unpaginated list - sales-form and the
     * quick-add-scan screen depend on having every product loaded client-side for
     * instant in-memory barcode-exact-match lookups without a network round-trip per
     * scan, so that endpoint's behavior must not change.
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductsPage(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

        log.info("GET /api/products/page - pharmacyId: {}, page: {}, size: {}, search: '{}', category: '{}'",
                pharmacyId, page, size, search, category);

        Page<ProductResponse> products = productService.getProductsPage(
                pharmacyId, page, size, search, category, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(products, "Products retrieved successfully"));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getProductsCount(
            @RequestParam Long pharmacyId) {

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

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

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

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

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

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

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

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

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

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

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

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

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

        log.info("GET /api/products/low-stock - pharmacyId: {}", pharmacyId);

        List<ProductResponse> products = productService.getLowStockProducts(pharmacyId);
        return ResponseEntity.ok(
                ApiResponse.success(products, "Low stock products retrieved")
        );
    }

    @PostMapping("/calculate-sell-price")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateSellPrice(
            @RequestParam BigDecimal buyPrice,
            @RequestParam(defaultValue = "25") int profitMarginPercent) {

        log.info("Calculating sell price: buyPrice={}, margin={}%", buyPrice, profitMarginPercent);

        BigDecimal profitMargin = BigDecimal.valueOf(profitMarginPercent).divide(BigDecimal.valueOf(100));
        BigDecimal sellPrice = buyPrice.multiply(BigDecimal.ONE.add(profitMargin))
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> response = new HashMap<>();
        response.put("buyPrice", buyPrice);
        response.put("profitMarginPercent", profitMarginPercent);
        response.put("sellPrice", sellPrice);
        response.put("profitAmount", sellPrice.subtract(buyPrice));

        return ResponseEntity.ok(ApiResponse.success(response, "Sell price calculated successfully"));
    }
}