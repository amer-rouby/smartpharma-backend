package com.smartpharma.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

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

    private String customerPhone;

    private String paymentMethod = "CASH";

    @DecimalMin(value = "0", message = "Discount cannot be negative")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemRequest {
        @NotNull
        private Long productId;

        @NotNull
        @Min(1)
        private Integer quantity;

        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal unitPrice;
    }
}
