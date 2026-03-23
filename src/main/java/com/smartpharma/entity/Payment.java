package com.smartpharma.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartpharma.entity.enums.PaymentMethod;
import com.smartpharma.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", schema = "smartpharma", indexes = {
        @Index(name = "idx_payment_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_payment_reference", columnList = "reference_number"),
        @Index(name = "idx_payment_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    @JsonIgnore
    private Pharmacy pharmacy;

    @Column(name = "pharmacy_id", insertable = false, updatable = false)
    private Long pharmacyId;

    @Column(name = "reference_number", unique = true, nullable = false, columnDefinition = "VARCHAR(255)")
    private String referenceNumber;

    @Column(name = "payment_method", nullable = false, columnDefinition = "VARCHAR(50)")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(50)")
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "gateway_transaction_id", columnDefinition = "VARCHAR(255)")
    private String gatewayTransactionId;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "customer_name", columnDefinition = "VARCHAR(100)")
    private String customerName;

    @Column(name = "customer_phone", columnDefinition = "VARCHAR(20)")
    private String customerPhone;

    @Column(name = "customer_email", columnDefinition = "VARCHAR(100)")
    private String customerEmail;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED;
    }
}