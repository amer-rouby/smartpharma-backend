package com.smartpharma.dto.response;

import lombok.*;
import org.hibernate.Hibernate;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productBarcode;
    private Integer quantity;
    private Integer receivedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String notes;
    private boolean fullyReceived;
    private int pendingQuantity;

    public static PurchaseOrderItemResponse fromEntity(com.smartpharma.entity.PurchaseOrderItem item) {
        // ✅ معالجة آمنة للمنتج لو محذوف أو مش موجود
        String productName = "منتج غير متاح";
        String productBarcode = null;
        Long productId = null;

        try {
            if (item.getProduct() != null) {
                // التحقق لو الـ product محمل أو لأ
                if (!Hibernate.isInitialized(item.getProduct())) {
                    // لو مش محمل، نحاول نحمّله
                    Hibernate.initialize(item.getProduct());
                }

                if (item.getProduct() != null) {
                    productId = item.getProduct().getId();
                    productName = item.getProduct().getName() != null ?
                            item.getProduct().getName() : "منتج غير متاح";
                    productBarcode = item.getProduct().getBarcode();
                }
            }
        } catch (Exception e) {
            // لو حصل أي خطأ ( زي EntityNotFoundException)، نستخدم القيم الافتراضية
            productName = "منتج غير متاح";
        }

        return PurchaseOrderItemResponse.builder()
                .id(item.getId())
                .productId(productId)
                .productName(productName)
                .productBarcode(productBarcode)
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