package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "anomaly_detections", schema = "smartpharma", indexes = {
        @Index(name = "idx_anomaly_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_anomaly_status", columnList = "status"),
        @Index(name = "idx_anomaly_dedup", columnList = "pharmacy_id, type, related_entity_type, related_entity_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyDetection {

    // Wording is deliberately neutral - "unusual activity", never "fraud" - and
    // nothing here auto-blocks or auto-accuses a user. This is a rule-based
    // surface for a pharmacist/admin to review, not an automated judgment.
    public enum Type {
        EXCESSIVE_RETURNS,
        UNUSUAL_DISCOUNT,
        FREQUENT_CANCELLATIONS,
        REPEATED_STOCK_ADJUSTMENTS,
        LARGE_STOCK_DISCREPANCY
    }

    public enum Severity {
        LOW, MEDIUM, HIGH
    }

    public enum Status {
        NEW, REVIEWED, DISMISSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Severity severity = Severity.LOW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.NEW;

    @Column(nullable = false, length = 500)
    private String description;

    // Polymorphic reference to whatever the anomaly is about (a user, a batch, etc.)
    @Column(name = "related_entity_type", length = 40)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "related_entity_name", length = 255)
    private String relatedEntityName;

    @CreationTimestamp
    @Column(name = "detected_at", updatable = false)
    private LocalDateTime detectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
