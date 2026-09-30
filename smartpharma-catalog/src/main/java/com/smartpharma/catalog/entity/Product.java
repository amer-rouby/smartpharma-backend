package com.smartpharma.catalog.entity;


import com.smartpharma.catalog.util.IngredientKey;
import com.smartpharma.common.entity.Pharmacy;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
        @Index(name = "idx_products_code", columnList = "code"),
        @Index(name = "idx_products_ingredient", columnList = "pharmacy_id, ingredient_key")
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

    // What the pharmacist typed as the active ingredient; null = take it from
    // the scientific name.
    @Column(name = "active_ingredient", length = 255)
    private String activeIngredient;

    // Normalized substance(s) (IngredientKey): products sharing it are
    // alternatives for each other. Always derived, never edited directly.
    @Column(name = "ingredient_key", length = 255)
    private String ingredientKey;

    @Column(length = 100)
    private String barcode;

    @Column(length = 50, unique = true)
    private String code;

    @Column(length = 100)
    private String category;

    // ETA e-receipt item coding: GS1 or EGS plus the code registered with ETA.
    // When empty, a barcode that is a valid GTIN is sent as GS1 instead.
    @Column(name = "eta_item_type", length = 10)
    private String etaItemType;

    @Column(name = "eta_item_code", length = 100)
    private String etaItemCode;

    // VAT (ETA tax type T1) subtype: V009 general, V010 other rate, V003
    // exempt, V004 not subject. Null = use the pharmacy's ETA default.
    @Column(name = "eta_tax_subtype", length = 10)
    private String etaTaxSubtype;

    // Percentage, only for V010 (V009 is always 14).
    @Column(name = "eta_tax_rate", precision = 5, scale = 2)
    private BigDecimal etaTaxRate;

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
    @JsonIgnoreProperties({"product"})
    private List<StockBatch> stockBatches = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void deriveIngredientKey() {
        ingredientKey = IngredientKey.of(activeIngredient != null ? activeIngredient : scientificName);
    }

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

    @Transient
    public boolean isLowStock() {
        Integer total = getTotalStock();
        return total != null && total <= minStockLevel;
    }

    @Transient
    public boolean isOutOfStock() {
        Integer total = getTotalStock();
        return total != null && total == 0;
    }
}