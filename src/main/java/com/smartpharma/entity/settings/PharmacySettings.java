package com.smartpharma.entity.settings;

import com.smartpharma.entity.Pharmacy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pharmacy_settings", schema = "smartpharma")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false, unique = true)
    private Pharmacy pharmacy;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String licenseNumber;

    @Column(length = 50)
    private String taxNumber;

    @Column(length = 100)
    private String commercialRegister;

    @Column(length = 255)
    private String logoUrl;

    @Column(length = 20)
    @Builder.Default
    private String currency = "EGP";

    @Column(length = 50)
    @Builder.Default
    private String timezone = "Africa/Cairo";

    @Column(length = 20)
    @Builder.Default
    private String dateFormat = "dd/MM/yyyy";

    @Column(length = 20)
    @Builder.Default
    private String timeFormat = "24h";

    @Column(length = 255)
    @Builder.Default
    private String enabledPaymentMethods = "CASH,VISA,MASTERCARD,INSTAPAY,FAWRY,WALLET,BANK_TRANSFER";

    // What counts as a "large" sale/expense for the notifyLargeSale/notifyLargeExpense
    // per-user alert preferences - admin-configured per pharmacy since what's "large"
    // varies a lot between a small and a busy pharmacy.
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal largeSaleThreshold = BigDecimal.valueOf(5000);

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal largeExpenseThreshold = BigDecimal.valueOf(2000);

    // Whether the sales screen must block checkout on prescription-required
    // products until a prescription photo is attached. Defaults to on (the
    // existing behavior); admins can turn it off to speed up checkout.
    // Nullable (not NOT NULL): ddl-auto=update can't add a NOT NULL column to a
    // table that already has rows without a default clause, so treat null as
    // true wherever this is read.
    @Builder.Default
    private Boolean requirePrescriptionUpload = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}