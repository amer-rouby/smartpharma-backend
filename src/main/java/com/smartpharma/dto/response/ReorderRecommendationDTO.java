package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRecommendationDTO {
    private Long predictionId;
    private Long productId;
    private String productName;
    private String productCode;
    private Integer currentStock;
    private Integer recommendedQuantity;
    private Long supplierId;
    private String supplierName;
    private String reason;
    private String priority;
    private String riskLevel;
    private Integer daysUntilStockout;
}
