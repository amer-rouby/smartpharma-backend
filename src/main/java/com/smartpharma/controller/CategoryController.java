package com.smartpharma.controller;

import com.smartpharma.dto.request.CategoryRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.CategoryResponse;
import com.smartpharma.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories(
            @RequestParam Long pharmacyId) {

        log.info("GET /api/categories - pharmacyId: {}", pharmacyId);

        List<CategoryResponse> categories = categoryService.getAllCategories(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories retrieved successfully"));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCategoriesCount(
            @RequestParam Long pharmacyId) {

        log.info("GET /api/categories/count - pharmacyId: {}", pharmacyId);

        Long count = categoryService.getCategoriesCount(pharmacyId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(ApiResponse.success(response, "Categories count retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        log.info("GET /api/categories/{} - pharmacyId: {}", id, pharmacyId);

        CategoryResponse category = categoryService.getCategory(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(category, "Category retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) {

        log.info("POST /api/categories - pharmacyId: {}, categoryName: {}",
                request.getPharmacyId(), request.getName());

        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success(category, "Category created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            @RequestParam Long pharmacyId) {

        log.info("PUT /api/categories/{} - pharmacyId: {}", id, pharmacyId);

        CategoryResponse category = categoryService.updateCategory(id, request, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(category, "Category updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        log.info("DELETE /api/categories/{} - pharmacyId: {}", id, pharmacyId);

        categoryService.deleteCategory(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> searchCategories(
            @RequestParam Long pharmacyId,
            @RequestParam String query) {

        log.info("GET /api/categories/search - pharmacyId: {}, query: '{}'", pharmacyId, query);

        List<CategoryResponse> categories = categoryService.searchCategories(pharmacyId, query);
        return ResponseEntity.ok(ApiResponse.success(categories, "Search completed successfully"));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories(
            @RequestParam Long pharmacyId) {

        log.info("GET /api/categories/active - pharmacyId: {}", pharmacyId);

        List<CategoryResponse> categories = categoryService.getActiveCategories(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(categories, "Active categories retrieved successfully"));
    }
}