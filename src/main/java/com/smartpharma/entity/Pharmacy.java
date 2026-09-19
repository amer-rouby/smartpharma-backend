package com.smartpharma.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pharmacies")
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Pharmacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(unique = true, length = 100)
    private String licenseNumber;

    @Column(length = 500)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PlanType planType = PlanType.BASIC;

    // Set only once an activation/renewal code has actually been applied (see
    // com.smartpharma.license.service.impl.LicenseServiceImpl) - null means
    // never licensed yet, treated as unlimited so this never retroactively
    // locks out a pharmacy nobody has issued a code to.
    @Column(name = "subscription_expires_at")
    private Instant subscriptionExpiresAt;

    // Watermark that only ever moves forward - if the system clock is ever
    // seen behind this, the pharmacy is treated as locked regardless of
    // subscriptionExpiresAt, so rolling the clock back can't "un-expire" a
    // lapsed subscription. Internal bookkeeping only, never exposed via API.
    @Column(name = "license_last_seen_at")
    private Instant licenseLastSeenAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "pharmacy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnoreProperties({"pharmacy"})
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "pharmacy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnoreProperties({"pharmacy"})
    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "pharmacy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnoreProperties({"pharmacy"})
    private List<Payment> payments = new ArrayList<>();

    public enum SubscriptionStatus {
        TRIAL, ACTIVE, SUSPENDED, CANCELLED
    }

    public enum PlanType {
        BASIC, PROFESSIONAL, ENTERPRISE
    }
}