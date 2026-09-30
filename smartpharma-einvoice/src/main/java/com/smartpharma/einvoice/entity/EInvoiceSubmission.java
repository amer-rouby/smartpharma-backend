package com.smartpharma.einvoice.entity;


import com.smartpharma.sales.entity.SaleReturn;
import com.smartpharma.sales.entity.SaleTransaction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// An ETA e-receipt and its submission state: the sales receipt of a sale
// and, if the sale is later cancelled, its return receipt (a second row for
// the same sale, pointing at the first). A retry updates the same row and
// bumps retryCount rather than creating a new submission each time.
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

    // SALE = ETA receipt type "s", RETURN = type "r".
    public enum DocumentType {
        SALE, RETURN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Not unique any more (a sale can have a SALE and a RETURN row); databases
    // created before returns keep the old unique constraint until
    // EInvoiceReturnReceiptBackfill drops it at startup.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_transaction_id", nullable = false)
    @ToString.Exclude
    private SaleTransaction saleTransaction;

    // Nullable for rows written before returns existed; the backfill sets them to SALE.
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 10)
    @Builder.Default
    private DocumentType documentType = DocumentType.SALE;

    // For a RETURN: the SALE receipt it reverses.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_submission_id")
    @ToString.Exclude
    private EInvoiceSubmission originalSubmission;

    // For a RETURN of some of the items (a SaleReturn); null when it reverses
    // the whole receipt because the sale was cancelled.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_return_id")
    @ToString.Exclude
    private SaleReturn saleReturn;

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
