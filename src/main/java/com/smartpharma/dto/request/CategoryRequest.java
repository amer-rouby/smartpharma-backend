package com.smartpharma.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "اسم التصنيف مطلوب")
    private String name;

    private String description;

    private String icon;

    @Builder.Default
    private String color = "#667eea";

    @NotNull(message = "الصيدلية مطلوبة")
    private Long pharmacyId;

    @Builder.Default
    private Boolean isActive = true;
}