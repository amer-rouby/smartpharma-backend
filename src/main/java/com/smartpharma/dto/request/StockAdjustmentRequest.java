// src/main/java/com/smartpharma/dto/request/StockAdjustmentRequest.java

package com.smartpharma.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull
    private Integer quantity;  // Positive for add, negative for deduct

    private String reason;

    private String reference;  // Optional reference number
}