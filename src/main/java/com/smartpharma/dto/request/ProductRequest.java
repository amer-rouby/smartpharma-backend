package com.smartpharma.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
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

    @NotNull(message = "Sell price is required")
    @Positive(message = "Sell price must be greater than zero")
    private BigDecimal sellPrice;

    private BigDecimal buyPrice;

    private Map<String, Object> extraAttributes;
    @JsonProperty("initialStock")
    private Integer initialStock;

    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;
}