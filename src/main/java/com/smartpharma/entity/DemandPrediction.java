package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity for demand prediction records.
 */
@Entity
@Table(name = "demand_predictions", schema = "smartpharma", indexes = {
        @Index(name = "idx_predictions_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_predictions_product_date", columnList = "product_id, prediction_date")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DemandPrediction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "prediction_date", nullable = false)
    private LocalDate predictionDate;

    // Maps to DB column 'predicted_demand'
    @Column(name = "predicted_demand", nullable = false)
    private Integer predictedQuantity;

    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    @Column(name = "accuracy_percentage", precision = 5, scale = 2)
    private BigDecimal accuracyPercentage;

    @Column(name = "confidence_level", precision = 5, scale = 2)
    private BigDecimal confidenceLevel;

    @Column(name = "algorithm_version", length = 50)
    private String algorithmVersion;

    @Column(name = "factors_applied", columnDefinition = "jsonb")
    private String factorsApplied;

    @Column(name = "is_consumed")
    @Builder.Default
    private Boolean isConsumed = false;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper: check if actual sales data exists
    public boolean isRealized() {
        return actualQuantity != null;
    }

    // Helper: check if prediction date is in the past
    public boolean isExpired() {
        return predictionDate != null && predictionDate.isBefore(LocalDate.now());
    }

    // Calculate accuracy percentage based on actual vs predicted
    public BigDecimal calculateAccuracy() {
        if (actualQuantity == null || predictedQuantity == null || predictedQuantity == 0) {
            return null;
        }
        int error = Math.abs(actualQuantity - predictedQuantity);
        double accuracy = Math.max(0, 100.0 - (error * 100.0 / predictedQuantity));
        return BigDecimal.valueOf(accuracy).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}