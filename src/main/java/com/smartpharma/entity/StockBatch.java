package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_batches", indexes = {
        @Index(name = "idx_batches_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_batches_expiry", columnList = "expiry_date"),
        @Index(name = "idx_batches_product", columnList = "product_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @Column(length = 100)
    private String batchNumber;

    @Column(nullable = false)
    private Integer quantityCurrent = 0;

    @Column(nullable = false)
    private Integer quantityInitial = 0;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal buyPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellPrice;

    @Column(length = 50)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BatchStatus status = BatchStatus.ACTIVE;

    @Version
    private Long version; // Optimistic Locking

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum BatchStatus {
        ACTIVE, EXPIRED, SOLD_OUT, RETURNED
    }

    public boolean isExpiringSoon(int daysThreshold) {
        return expiryDate.isBefore(LocalDate.now().plusDays(daysThreshold));
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now());
    }
}