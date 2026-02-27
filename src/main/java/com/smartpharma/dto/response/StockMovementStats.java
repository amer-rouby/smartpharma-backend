package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementStats {

    private Long totalMovements;
    private Integer totalStockIn;
    private Integer totalStockOut;
    private Integer totalAdjustments;
    private Integer totalExpired;
    private Integer totalTransferred;
}