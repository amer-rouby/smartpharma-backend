package com.smartpharma.repository;

import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Pharmacy.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

    // ================================
    // ✅ Basic Find Methods
    // ================================

    Optional<Pharmacy> findByLicenseNumber(String licenseNumber);

    Optional<Pharmacy> findByEmail(String email);

    // ✅ FIXED: Added missing method
    Optional<Pharmacy> findByIdAndDeletedAtIsNull(Long id);

    List<Pharmacy> findBySubscriptionStatus(SubscriptionStatus status);

    // ================================
    // ✅ Custom Queries
    // ================================

    @Query("SELECT p FROM Pharmacy p WHERE p.subscriptionStatus = 'ACTIVE' AND p.deletedAt IS NULL")
    List<Pharmacy> findActivePharmacies();

    @Query("SELECT p FROM Pharmacy p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Pharmacy> findByIdAndActive(@Param("id") Long id);

    // ================================
    // ✅ Exists Methods
    // ================================

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByEmail(String email);

    // ================================
    // ✅ Count Methods
    // ================================

    @Query("SELECT COUNT(p) FROM Pharmacy p WHERE p.deletedAt IS NULL")
    Long countActivePharmacies();

    @Query("SELECT COUNT(p) FROM Pharmacy p WHERE p.subscriptionStatus = :status AND p.deletedAt IS NULL")
    Long countBySubscriptionStatus(@Param("status") SubscriptionStatus status);
}