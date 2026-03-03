package com.smartpharma.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StockAdjustmentRequest {
    @NotNull
    private String type;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    private String reason;

    private String notes;
}