package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items", schema = "smartpharma", indexes = {
        @Index(name = "idx_poi_order", columnList = "purchase_order_id"),
        @Index(name = "idx_poi_product", columnList = "product_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "received_quantity")
    @Builder.Default
    private Integer receivedQuantity = 0;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ✅ FIXED: calculateTotal مع default value
    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        if (unitPrice != null && quantity != null && quantity > 0) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        } else {
            this.totalPrice = BigDecimal.ZERO;  // ✅ default value عشان ما ترجعش null
        }
    }

    // ✅ FIXED: getter آمن
    public BigDecimal getTotalPrice() {
        if (totalPrice == null) {
            calculateTotal();  // ✅ احسبها لو مش محسوبة
        }
        return totalPrice;
    }

    public boolean isFullyReceived() {
        return receivedQuantity != null && receivedQuantity >= quantity;
    }

    public int getPendingQuantity() {
        return quantity - (receivedQuantity != null ? receivedQuantity : 0);
    }
}