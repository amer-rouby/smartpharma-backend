package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private Long pharmacyId;
    private String name;
    private String scientificName;
    private String barcode;
    private String category;
    private String unitType;
    private Integer minStockLevel;
    private Boolean prescriptionRequired;
    private BigDecimal sellPrice;
    private BigDecimal buyPrice;
    private Map<String, Object> extraAttributes;
    private Integer totalStock;
    private LocalDateTime createdAt;
}