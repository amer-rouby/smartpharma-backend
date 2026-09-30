package com.smartpharma.einvoice.entity;

import com.smartpharma.einvoice.entity.enums.EtaEnvironment;
import com.smartpharma.common.entity.Pharmacy;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// The pharmacy as an ETA taxpayer: the seller block of every receipt plus the
// ERP API credentials. Each pharmacy is its own taxpayer, so credentials are
// per pharmacy (encrypted), not a single server-wide setting.
@Entity
@Table(name = "eta_taxpayer_profiles", schema = "smartpharma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtaTaxpayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false, unique = true)
    @ToString.Exclude
    private Pharmacy pharmacy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private EtaEnvironment environment = EtaEnvironment.PREPROD;

    // Tax registration number - 9 digits.
    @Column(length = 30)
    private String rin;

    @Column(name = "company_trade_name", length = 200)
    private String companyTradeName;

    @Column(name = "branch_code", length = 50)
    @Builder.Default
    private String branchCode = "0";

    @Column(name = "activity_code", length = 10)
    private String activityCode;

    @Column(length = 100)
    private String governate;

    @Column(name = "region_city", length = 100)
    private String regionCity;

    @Column(length = 200)
    private String street;

    @Column(name = "building_number", length = 100)
    private String buildingNumber;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    @Column(name = "client_id", length = 100)
    private String clientId;

    @Column(name = "client_secret_enc", length = 500)
    @ToString.Exclude
    private String clientSecretEncrypted;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
