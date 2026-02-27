package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements", schema = "smartpharma", indexes = {
        @Index(name = "idx_movements_batch", columnList = "batch_id"),
        @Index(name = "idx_movements_type", columnList = "movement_type"),
        @Index(name = "idx_movements_date", columnList = "movement_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private StockBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_value", precision = 10, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "movement_date", nullable = false, updatable = false)
    private LocalDateTime movementDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "pharmacy_id", nullable = false)
    private Long pharmacyId;

    public enum MovementType {
        STOCK_IN,           // وارد مخزن (شراء، إرجاع)
        STOCK_OUT,          // صادر مخزن (بيع، هالك)
        STOCK_ADJUSTMENT,   // تعديل مخزن
        TRANSFER_IN,        // تحويل وارد
        TRANSFER_OUT,       // تحويل صادر
        EXPIRED,            // انتهاء صلاحية
        DISCARDED           // تلف/إتلاف
    }
}