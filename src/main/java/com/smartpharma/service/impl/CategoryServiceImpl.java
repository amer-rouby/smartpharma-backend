package com.smartpharma.service.impl;

import com.smartpharma.dto.request.CategoryRequest;
import com.smartpharma.dto.response.CategoryResponse;
import com.smartpharma.entity.Category;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.repository.CategoryRepository;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final PharmacyRepository pharmacyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(Long pharmacyId) {
        return categoryRepository.findByPharmacyId(pharmacyId)
                .stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long id, Long pharmacyId) {
        Category category = categoryRepository.findByIdAndPharmacyId(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return CategoryResponse.fromEntity(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        // Check if category with same name exists
        if (categoryRepository.existsByPharmacyIdAndNameIgnoreCase(request.getPharmacyId(), request.getName())) {
            throw new RuntimeException("Category with this name already exists");
        }

        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor() != null ? request.getColor() : "#667eea")
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .pharmacy(pharmacy)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: {} for pharmacy {}", saved.getName(), pharmacy.getId());
        return CategoryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request, Long pharmacyId) {
        Category category = categoryRepository.findByIdAndPharmacyId(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Check if new name conflicts with existing category
        if (!category.getName().equalsIgnoreCase(request.getName()) &&
                categoryRepository.existsByPharmacyIdAndNameIgnoreCase(pharmacyId, request.getName())) {
            throw new RuntimeException("Category with this name already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor() != null ? request.getColor() : category.getColor());
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : category.getIsActive());

        Category updated = categoryRepository.save(category);
        log.info("Category updated: {} for pharmacy {}", updated.getName(), pharmacyId);
        return CategoryResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id, Long pharmacyId) {
        Category category = categoryRepository.findByIdAndPharmacyId(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Soft delete
        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);
        log.info("Category deleted (soft): {} for pharmacy {}", category.getName(), pharmacyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> searchCategories(Long pharmacyId, String query) {
        return categoryRepository.searchByPharmacyIdAndName(pharmacyId, query)
                .stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCategoriesCount(Long pharmacyId) {
        return categoryRepository.countByPharmacyId(pharmacyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories(Long pharmacyId) {
        return categoryRepository.findActiveByPharmacyId(pharmacyId)
                .stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }
}