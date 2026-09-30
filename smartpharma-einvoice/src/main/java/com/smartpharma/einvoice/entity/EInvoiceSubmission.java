package com.smartpharma.einvoice.entity;


import com.smartpharma.sales.entity.SaleTransaction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// The ETA e-receipt issued for a sale and its submission state. One row per
// sale (unique FK) - a retry updates the same row and bumps retryCount rather
// than creating a new submission each time.
//
// receiptJson is the exact receipt text sent to ETA: the UUID is a hash of
// it, so it's stored once at issue time and never rebuilt from the sale.
// PENDING = issued, waiting to be sent; ERROR = not issued or not delivered
// (retryable); REJECTED = ETA refused it, needs a corrected re-issue.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pos_device_id")
    @ToString.Exclude
    private EtaPosDevice posDevice;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    // UTC, formatted exactly as sent (yyyy-MM-ddTHH:mm:ssZ).
    @Column(name = "date_time_issued", length = 25)
    private String dateTimeIssued;

    @Column(name = "previous_uuid", length = 64)
    private String previousUuid;

    @Column(name = "receipt_json", columnDefinition = "TEXT")
    @ToString.Exclude
    private String receiptJson;

    @Column(name = "qr_content", length = 500)
    private String qrContent;

    @Column(name = "submission_uuid", length = 50)
    private String submissionUuid;

    @Column(name = "long_id", length = 200)
    private String longId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // Existing column is VARCHAR(500) and ddl-auto=update never widens it,
    // so recordError() truncates instead.
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

    public void recordError(Status status, String message) {
        this.status = status;
        this.errorMessage = message == null || message.length() <= 500 ? message : message.substring(0, 497) + "...";
    }

    public boolean isIssued() {
        return receiptJson != null && etaUuid != null;
    }
}
