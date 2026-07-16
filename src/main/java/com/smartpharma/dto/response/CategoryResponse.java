package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private String nameAr;
    private String nameEn;
    private String description;
    private String icon;
    private String color;
    private Boolean isActive;
    private Long pharmacyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CategoryResponse fromEntity(com.smartpharma.entity.Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .nameAr(category.getNameAr())
                .nameEn(category.getNameEn())
                .description(category.getDescription())
                .icon(category.getIcon())
                .color(category.getColor())
                .isActive(category.getIsActive())
                .pharmacyId(category.getPharmacy().getId())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
