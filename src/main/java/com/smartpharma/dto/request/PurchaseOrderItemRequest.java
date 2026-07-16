package com.smartpharma.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseOrderItemRequest {

    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    private String productName;

    @NotNull(message = "Unit price is required")
    @Min(value = 0, message = "Unit price must be 0 or more")
    private BigDecimal unitPrice;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}