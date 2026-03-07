package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "share_links", schema = "smartpharma")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private String entityType; // "PREDICTION", "REPORT", etc.

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private Long pharmacyId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "access_count")
    private Integer accessCount = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    public void generateToken() {
        if (this.token == null) {
            this.token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void incrementAccessCount() {
        this.accessCount = (this.accessCount == null) ? 1 : this.accessCount + 1;
    }
}