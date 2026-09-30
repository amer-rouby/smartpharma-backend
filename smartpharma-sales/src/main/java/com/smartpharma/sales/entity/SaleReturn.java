package com.smartpharma.sales.entity;

import com.smartpharma.common.entity.Pharmacy;
import com.smartpharma.common.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Part (or all) of a sale's items brought back. The sale itself is never
// changed - its totals and ETA receipt stay what was sold; the refund is
// recorded here and summed into SaleTransaction.returnedAmount.
@Entity
@Table(name = "sale_returns", schema = "smartpharma",
        uniqueConstraints = @UniqueConstraint(name = "uk_sale_returns_number",
                columnNames = {"sale_transaction_id", "return_number"}),
        indexes = @Index(name = "idx_sale_returns_pharmacy", columnList = "pharmacy_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_transaction_id", nullable = false)
    @ToString.Exclude
    private SaleTransaction saleTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    @ToString.Exclude
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    // 1, 2, ... within the sale; also part of the ETA return receipt number.
    @Column(name = "return_number", nullable = false)
    private Integer returnNumber;

    // Shelf prices of the returned items.
    @Column(name = "items_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal itemsTotal;

    // The returned items' share of the sale-level discount.
    @Column(name = "discount_share", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountShare;

    // What the customer gets back: itemsTotal - discountShare.
    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    // False when the items went back damaged/expired and weren't put on the shelf.
    @Column(name = "restocked", nullable = false)
    private boolean restocked;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<SaleReturnItem> items = new ArrayList<>();
}
