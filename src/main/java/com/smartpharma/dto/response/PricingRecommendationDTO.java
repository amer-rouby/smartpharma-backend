package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRecommendationDTO {
    private Long productId;
    private String productName;
    private String productCode;
    private Long batchId;
    private String batchNumber;
    private LocalDate expiryDate;
    private Integer daysUntilExpiry;
    private Integer currentStock;
    private String reason;
    private Integer suggestedDiscountPercent;
    private String priority;
    private String message;
}
