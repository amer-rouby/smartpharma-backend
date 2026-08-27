package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// Tracks the Egyptian Tax Authority (ETA) e-invoice submission for a sale.
// One row per sale (unique FK) - a retry updates the same row and bumps
// retryCount rather than creating a new submission each time.
@Entity
@Table(name = "einvoice_submissions", schema = "smartpharma", indexes = {
        @Index(name = "idx_einvoice_sale", columnList = "sale_transaction_id"),
        @Index(name = "idx_einvoice_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EInvoiceSubmission {

    public enum Status {
        PENDING, SUBMITTED, ACCEPTED, REJECTED, ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_transaction_id", nullable = false, unique = true)
    private SaleTransaction saleTransaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "eta_uuid", length = 100)
    private String etaUuid;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
