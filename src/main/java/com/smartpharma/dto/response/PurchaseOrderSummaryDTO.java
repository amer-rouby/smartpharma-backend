package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderSummaryDTO {
    private Long purchaseOrderId;
    private String orderNumber;
    private Long productId;
    private String productName;
    private Integer quantity;
    private String status;
    private String message;
}