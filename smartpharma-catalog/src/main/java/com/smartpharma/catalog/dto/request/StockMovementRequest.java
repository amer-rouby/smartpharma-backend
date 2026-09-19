package com.smartpharma.catalog.dto.request;


import com.smartpharma.catalog.entity.StockMovement;
import com.smartpharma.catalog.entity.StockMovement.MovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementRequest {

    @NotNull(message = "Batch ID is required")
    private Long batchId;

    @NotNull(message = "Movement type is required")
    private MovementType movementType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private BigDecimal unitPrice;

    private String referenceNumber;

    private String reason;

    private String notes;
}