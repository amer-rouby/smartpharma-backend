package com.smartpharma.repository.settings;

import com.smartpharma.entity.settings.SmartFeatureSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmartFeatureSettingsRepository extends JpaRepository<SmartFeatureSettings, Long> {

    @Query("SELECT s FROM SmartFeatureSettings s WHERE s.pharmacy.id = :pharmacyId")
    Optional<SmartFeatureSettings> findByPharmacyId(@Param("pharmacyId") Long pharmacyId);
}
