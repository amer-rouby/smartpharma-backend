package com.smartpharma.entity.settings;

import com.smartpharma.entity.Pharmacy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Builder.Default
    private Boolean emailNotifications = true;

    @Builder.Default
    private Boolean smsNotifications = false;

    @Builder.Default
    private Boolean lowStockAlerts = true;

    @Builder.Default
    private Boolean expiryAlerts = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}