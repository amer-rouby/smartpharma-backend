package com.smartpharma.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String scientificName;
    private String barcode;
    private String category;
    private String unitType = "BOX";
    private Integer minStockLevel = 10;
    private Boolean prescriptionRequired = false;

    // ✅ حقول السعر
    @NotNull(message = "Sell price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Sell price must be greater than 0")
    private BigDecimal sellPrice;

    @DecimalMin(value = "0.0", message = "Buy price must be non-negative")
    private BigDecimal buyPrice;

    private Map<String, Object> extraAttributes;
}