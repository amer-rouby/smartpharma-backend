package com.smartpharma.einvoice.entity;

import com.smartpharma.common.entity.Pharmacy;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// A POS registered with ETA. Receipts from one device form a hash chain
// (each receipt carries the previous one's UUID), so lastReceiptUuid is the
// head of that chain and is only moved while holding a row lock - see
// EtaPosDeviceRepository.findActiveForUpdate.
@Entity
@Table(name = "eta_pos_devices", schema = "smartpharma", indexes = {
        @Index(name = "idx_eta_device_pharmacy", columnList = "pharmacy_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtaPosDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    @ToString.Exclude
    private Pharmacy pharmacy;

    @Column(name = "serial_number", nullable = false, length = 100)
    private String serialNumber;

    @Column(name = "os_version", nullable = false, length = 50)
    private String osVersion;

    @Column(name = "model_framework", nullable = false, length = 10)
    private String modelFramework;

    @Column(name = "preshared_key_enc", nullable = false, length = 500)
    @ToString.Exclude
    private String presharedKeyEncrypted;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // UUID of the last receipt issued from this device; empty until the first one.
    @Column(name = "last_receipt_uuid", length = 64)
    private String lastReceiptUuid;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
