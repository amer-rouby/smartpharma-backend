package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_transactions", schema = "smartpharma", indexes = {
        @Index(name = "idx_sales_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_sales_date", columnList = "transaction_date"),
        @Index(name = "idx_sales_invoice", columnList = "invoice_number"),
        @Index(name = "idx_sales_user", columnList = "user_id"),
        @Index(name = "idx_sales_deleted", columnList = "deleted_at")
})
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "invoice_number", unique = true, length = 100)
    private String invoiceNumber;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20, nullable = false)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "transaction_date", nullable = false)
    @Builder.Default
    private LocalDateTime transactionDate = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<SaleItem> items = new ArrayList<>();

    public enum PaymentMethod {
        CASH, VISA, INSTAPAY, WALLET, CREDIT
    }

    public void addItem(SaleItem item) {
        items.add(item);
        item.setTransaction(this);
        calculateTotals();
    }

    public void removeItem(SaleItem item) {
        items.remove(item);
        item.setTransaction(null);
        calculateTotals();
    }

    public void calculateTotals() {
        this.subtotal = items.stream()
                .map(SaleItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO;
        this.totalAmount = this.subtotal.subtract(discount).max(BigDecimal.ZERO);
    }

    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    @PrePersist
    @PreUpdate
    private void validateTotals() {
        if (this.subtotal == null) this.subtotal = BigDecimal.ZERO;
        if (this.discountAmount == null) this.discountAmount = BigDecimal.ZERO;
        if (this.totalAmount == null) {
            this.totalAmount = this.subtotal.subtract(this.discountAmount).max(BigDecimal.ZERO);
        }
    }
}