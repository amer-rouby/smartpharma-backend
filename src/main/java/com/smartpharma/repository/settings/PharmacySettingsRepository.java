package com.smartpharma.repository.settings;

import com.smartpharma.entity.settings.PharmacySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PharmacySettingsRepository extends JpaRepository<PharmacySettings, Long> {

    @Query("SELECT s FROM PharmacySettings s WHERE s.pharmacy.id = :pharmacyId")
    Optional<PharmacySettings> findByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    boolean existsByPharmacyId(@Param("pharmacyId") Long pharmacyId);
}