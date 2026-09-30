package com.smartpharma.sales.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_return_items", schema = "smartpharma",
        indexes = @Index(name = "idx_sale_return_items_item", columnList = "sale_item_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_return_id", nullable = false)
    @ToString.Exclude
    private SaleReturn saleReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_item_id", nullable = false)
    @ToString.Exclude
    private SaleItem saleItem;

    @Column(nullable = false)
    private Integer quantity;

    // The price it was sold at.
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}
