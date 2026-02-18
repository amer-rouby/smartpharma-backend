package com.smartpharma.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockBatchRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @Size(max = 100)
    private String batchNumber;

    @NotNull(message = "Initial quantity is required")
    @Min(1)
    private Integer quantityInitial;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    @NotNull(message = "Buy price is required")
    @DecimalMin(value = "0.01", message = "Buy price must be greater than 0")
    private BigDecimal buyPrice;

    @NotNull(message = "Sell price is required")
    @DecimalMin(value = "0.01", message = "Sell price must be greater than 0")
    private BigDecimal sellPrice;

    @Size(max = 50)
    private String location;
}