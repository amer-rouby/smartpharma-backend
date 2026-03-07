package com.smartpharma.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentHistoryDTO {
    private Long id;
    private Long batchId;
    private String batchNumber;
    private String productName;
    private String type;
    private Integer quantity;
    private String reason;
    private Integer previousQuantity;
    private Integer newQuantity;
    private String notes;
    private LocalDateTime adjustmentDate;
    private Long adjustedBy;
    private String adjustedByName;
}