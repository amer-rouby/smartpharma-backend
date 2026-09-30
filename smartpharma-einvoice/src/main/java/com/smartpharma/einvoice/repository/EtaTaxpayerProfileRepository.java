package com.smartpharma.einvoice.repository;

import com.smartpharma.einvoice.entity.EtaTaxpayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EtaTaxpayerProfileRepository extends JpaRepository<EtaTaxpayerProfile, Long> {

    @Query("SELECT p FROM EtaTaxpayerProfile p WHERE p.pharmacy.id = :pharmacyId")
    Optional<EtaTaxpayerProfile> findByPharmacyId(@Param("pharmacyId") Long pharmacyId);
}
