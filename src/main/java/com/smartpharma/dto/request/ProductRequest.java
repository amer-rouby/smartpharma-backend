package com.smartpharma.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "اسم المنتج مطلوب")
    private String name;

    private String scientificName;

    private String barcode;

    private String category;

    @Builder.Default
    private String unitType = "BOX";

    @Builder.Default
    private Integer minStockLevel = 10;

    @Builder.Default
    private Boolean prescriptionRequired = false;

    @NotNull(message = "سعر البيع مطلوب")
    @Positive(message = "سعر البيع يجب أن يكون أكبر من صفر")
    private BigDecimal sellPrice;

    private BigDecimal buyPrice;

    private Map<String, Object> extraAttributes;

    // ✅ ✅ ✅ إضافة حقل المخزون الأولي ✅ ✅ ✅
    @JsonProperty("initialStock")
    private Integer initialStock;
}