package com.smartpharma.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseOrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Integer receivedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String notes;
    private boolean fullyReceived;
    private int pendingQuantity;

    public static PurchaseOrderItemResponse fromEntity(com.smartpharma.entity.PurchaseOrderItem item) {
        return PurchaseOrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .receivedQuantity(item.getReceivedQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .notes(item.getNotes())
                .fullyReceived(item.isFullyReceived())
                .pendingQuantity(item.getPendingQuantity())
                .build();
    }
}