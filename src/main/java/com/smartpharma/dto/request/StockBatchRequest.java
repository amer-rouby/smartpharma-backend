// src/main/java/com/smartpharma/dto/request/StockBatchRequest.java

package com.smartpharma.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBatchRequest {

    // ✅ Required fields
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotBlank(message = "Batch number is required")
    private String batchNumber;

    @NotNull(message = "Initial quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityInitial;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    // ✅ Optional fields
    private LocalDate productionDate;  // ← ← ← ده اللي كان ناقص!

    private BigDecimal buyPrice;
    private BigDecimal sellPrice;

    private String location;
    private String shelf;
    private String warehouse;
    private String notes;

    // ✅ Helper: quantityCurrent defaults to quantityInitial if not provided
    public Integer getQuantityCurrent() {
        return quantityInitial;  // Or add separate field if needed
    }
}