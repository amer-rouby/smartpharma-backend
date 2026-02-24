// src/main/java/com/smartpharma/entity/User.java

package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"pharmacy_id", "username"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 255)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private UserRole role = UserRole.PHARMACIST;

    private Boolean isActive = true;

    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ================================
    // ✅ FIXED: Spring Security methods - IMPORTANT for @PreAuthorize
    // ================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ✅ مهم جداً: Spring Security بيلف على ROLE_ prefix
        // لو مفيش ROLE_ prefix، الـ @PreAuthorize("hasRole('ADMIN')") مش هيشغل
        String roleName = (role != null) ? role.name() : "PHARMACIST";
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + roleName)  // ← ROLE_ADMIN, ROLE_PHARMACIST, etc.
        );
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive != null && isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive != null && isActive;
    }

    // ================================
    // ✅ User Roles Enum
    // ================================

    public enum UserRole {
        ADMIN,
        PHARMACIST,
        MANAGER,
        VIEWER
    }
}