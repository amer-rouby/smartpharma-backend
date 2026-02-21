package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_transactions", schema = "smartpharma", indexes = {
        @Index(name = "idx_sales_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_sales_date", columnList = "transaction_date")
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
    @JoinColumn(name = "user_id")
    private User user;

    @Column(unique = true, length = 100)
    private String invoiceNumber;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(length = 20)
    private String customerPhone;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime transactionDate;

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
    }

    public void removeItem(SaleItem item) {
        items.remove(item);
        item.setTransaction(null);
    }

    public void calculateTotals() {
        this.subtotal = items.stream()
                .map(SaleItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO;
        this.totalAmount = this.subtotal.subtract(discount).max(BigDecimal.ZERO);
    }
}