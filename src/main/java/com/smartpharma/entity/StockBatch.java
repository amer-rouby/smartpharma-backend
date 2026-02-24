// src/main/java/com/smartpharma/entity/StockBatch.java

package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;  // ✅ مهم جداً لـ buyPrice و sellPrice
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_batches", indexes = {
        @Index(name = "idx_stock_batches_product", columnList = "product_id"),
        @Index(name = "idx_stock_batches_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_stock_batches_expiry", columnList = "expiry_date"),
        @Index(name = "idx_stock_batches_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;

    @Column(name = "quantity_current", nullable = false)
    private Integer quantityCurrent;

    @Column(name = "quantity_initial", nullable = false)
    private Integer quantityInitial;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    // ✅ FIXED: Added productionDate field
    @Column(name = "production_date")
    private LocalDate productionDate;

    @Column(name = "buy_price", precision = 10, scale = 2)
    private BigDecimal buyPrice;

    @Column(name = "sell_price", precision = 10, scale = 2)
    private BigDecimal sellPrice;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "shelf", length = 50)
    private String shelf;

    @Column(name = "warehouse", length = 100)
    private String warehouse;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BatchStatus status = BatchStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ================================
    // ✅ Helper methods
    // ================================

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon(int days) {
        if (expiryDate == null) return false;
        return expiryDate.isAfter(LocalDate.now()) &&
                expiryDate.isBefore(LocalDate.now().plusDays(days));
    }

    // ================================
    // ✅ Batch Status Enum
    // ================================

    public enum BatchStatus {
        ACTIVE,
        EXPIRED,
        DISCARDED
    }
}