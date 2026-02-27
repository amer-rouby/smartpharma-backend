package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products", schema = "smartpharma", indexes = {
        @Index(name = "idx_products_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_products_barcode", columnList = "barcode"),
        @Index(name = "idx_products_code", columnList = "code")
})
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String scientificName;

    @Column(length = 100)
    private String barcode;

    // ✅ FIXED: Added code field
    @Column(length = 50, unique = true)
    private String code;

    @Column(length = 100)
    private String category;

    @Column(length = 50)
    @Builder.Default
    private String unitType = "BOX";

    @Column
    @Builder.Default
    private Integer minStockLevel = 10;

    @Column(name = "is_prescription_required")
    @Builder.Default
    private Boolean prescriptionRequired = false;

    @Column(nullable = false, precision = 10, scale = 2, columnDefinition = "NUMERIC(10,2) DEFAULT 0.00")
    @Builder.Default
    private BigDecimal sellPrice = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, columnDefinition = "NUMERIC(10,2) DEFAULT 0.00")
    @Builder.Default
    private BigDecimal buyPrice = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> extraAttributes;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<StockBatch> stockBatches = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<DemandPrediction> predictions = new ArrayList<>();

    /**
     * ✅ يحسب إجمالي المخزون من الدفعات النشطة فقط
     */
    @Transient
    public Integer getTotalStock() {
        if (this.stockBatches == null) {
            return 0;
        }
        return stockBatches.stream()
                .filter(batch -> batch != null && batch.getQuantityCurrent() != null)
                .filter(batch -> batch.getStatus() == StockBatch.BatchStatus.ACTIVE)
                .mapToInt(StockBatch::getQuantityCurrent)
                .sum();
    }

    /**
     * ✅ يتحقق إذا كان المنتج منخفض المخزون
     */
    @Transient
    public boolean isLowStock() {
        Integer total = getTotalStock();
        return total != null && total <= minStockLevel;
    }

    /**
     * ✅ يتحقق إذا كان المنتج نفد منه المخزون
     */
    @Transient
    public boolean isOutOfStock() {
        Integer total = getTotalStock();
        return total != null && total == 0;
    }
}