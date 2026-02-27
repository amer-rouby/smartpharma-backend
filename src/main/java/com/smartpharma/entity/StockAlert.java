package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_alerts", indexes = {
        @Index(name = "idx_alerts_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_alerts_status", columnList = "status"),
        @Index(name = "idx_alerts_type", columnList = "alert_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private StockBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "severity", length = 20)
    @Builder.Default
    private String severity = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "UNREAD"; // UNREAD, READ, RESOLVED

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON data for additional info

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    public enum AlertType {
        LOW_STOCK,
        OUT_OF_STOCK,
        EXPIRING_SOON,
        EXPIRED,
        PRICE_CHANGE,
        STOCK_ADJUSTMENT
    }
}