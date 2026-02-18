package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "demand_predictions", indexes = {
        @Index(name = "idx_predictions_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_predictions_product_date", columnList = "product_id, prediction_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private LocalDate predictionDate;

    @Column(nullable = false)
    private Integer predictedQuantity;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(length = 50)
    private String algorithmVersion;

    @Column
    private Boolean isConsumed = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}