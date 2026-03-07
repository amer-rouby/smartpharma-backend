package com.smartpharma.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequest {

    @NotNull(message = "Pharmacy ID is required")
    private Long pharmacyId;

    @NotEmpty(message = "At least one item is required")
    private List<SaleItemRequest> items;
    private String notes;
    private String customerPhone;

    private String paymentMethod = "CASH";

    @DecimalMin(value = "0", message = "Discount cannot be negative")
    private BigDecimal discountAmount = BigDecimal.ZERO;
}