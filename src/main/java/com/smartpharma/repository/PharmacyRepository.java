package com.smartpharma.repository;

import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Pharmacy.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

    Optional<Pharmacy> findByLicenseNumber(String licenseNumber);

    Optional<Pharmacy> findByEmail(String email);

    List<Pharmacy> findBySubscriptionStatus(SubscriptionStatus status);

    @Query("SELECT p FROM Pharmacy p WHERE p.subscriptionStatus = 'ACTIVE' AND p.deletedAt IS NULL")
    List<Pharmacy> findActivePharmacies();

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByEmail(String email);
}