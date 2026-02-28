package com.smartpharma.service;

import com.smartpharma.dto.request.CategoryRequest;
import com.smartpharma.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories(Long pharmacyId);

    CategoryResponse getCategory(Long id, Long pharmacyId);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request, Long pharmacyId);

    void deleteCategory(Long id, Long pharmacyId);

    List<CategoryResponse> searchCategories(Long pharmacyId, String query);

    Long getCategoriesCount(Long pharmacyId);

    List<CategoryResponse> getActiveCategories(Long pharmacyId);
}