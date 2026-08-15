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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public Page<CategoryResponse> getCategoriesPage(Long pharmacyId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.ASC, "nameAr"));
        return categoryRepository.searchAndFilter(pharmacyId, search, pageable)
                .map(CategoryResponse::fromEntity);
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
        String categoryName = resolveCategoryName(request);
        String nameAr = resolveNameAr(request, categoryName);
        String nameEn = resolveNameEn(request, categoryName);

        // Check if category with same name exists
        if (categoryRepository.existsByPharmacyIdAndNameIgnoreCase(request.getPharmacyId(), categoryName)) {
            throw new RuntimeException("Category with this name already exists");
        }

        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        Category category = Category.builder()
                .name(categoryName)
                .nameAr(nameAr)
                .nameEn(nameEn)
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
        String categoryName = resolveCategoryName(request);
        String nameAr = resolveNameAr(request, categoryName);
        String nameEn = resolveNameEn(request, categoryName);

        // Check if new name conflicts with existing category
        if (!category.getName().equalsIgnoreCase(categoryName) &&
                categoryRepository.existsByPharmacyIdAndNameIgnoreCase(pharmacyId, categoryName)) {
            throw new RuntimeException("Category with this name already exists");
        }

        category.setName(categoryName);
        category.setNameAr(nameAr);
        category.setNameEn(nameEn);
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

    private String resolveCategoryName(CategoryRequest request) {
        String name = cleanText(request.getName());
        if (name != null) {
            return name;
        }

        String nameAr = cleanText(request.getNameAr());
        if (nameAr != null) {
            return nameAr;
        }

        return cleanText(request.getNameEn());
    }

    private String resolveNameAr(CategoryRequest request, String fallbackName) {
        String nameAr = cleanText(request.getNameAr());
        return nameAr != null ? nameAr : fallbackName;
    }

    private String resolveNameEn(CategoryRequest request, String fallbackName) {
        String nameEn = cleanText(request.getNameEn());
        return nameEn != null ? nameEn : fallbackName;
    }

    private String cleanText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
