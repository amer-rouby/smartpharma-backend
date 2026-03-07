package com.smartpharma.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdatePredictionDTO {
    private Integer predictedQuantity;
    private BigDecimal confidenceLevel;
    private String recommendation;
    private String notes;
}