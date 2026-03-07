package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "purchase_orders", schema = "smartpharma", indexes = {
        @Index(name = "idx_po_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_po_supplier", columnList = "supplier_id"),
        @Index(name = "idx_po_status", columnList = "status"),
        @Index(name = "idx_po_date", columnList = "order_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", length = 50, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @Column(length = 20)
    @Builder.Default
    private String priority = "NORMAL";

    @Column(name = "payment_terms", length = 50)
    private String paymentTerms;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();

    // ✅ FIXED: Helper methods آمنة
    public void addItem(PurchaseOrderItem item) {
        items.add(item);
        item.setPurchaseOrder(this);
        recalculateTotal();
    }

    public void removeItem(PurchaseOrderItem item) {
        items.remove(item);
        item.setPurchaseOrder(null);
        recalculateTotal();
    }

    // ✅ FIXED: recalculateTotal مع null safety
    public void recalculateTotal() {
        if (items == null || items.isEmpty()) {
            this.totalAmount = BigDecimal.ZERO;
            return;
        }

        this.totalAmount = items.stream()
                .map(PurchaseOrderItem::getTotalPrice)
                .filter(Objects::nonNull)  // ✅ فلتر الـ null values
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isDraft() { return "DRAFT".equals(status); }
    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isApproved() { return "APPROVED".equals(status); }
    public boolean isReceived() { return "RECEIVED".equals(status); }
    public boolean isCancelled() { return "CANCELLED".equals(status); }
}