package com.smartpharma.einvoice.repository;

import com.smartpharma.einvoice.entity.EtaPosDevice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EtaPosDeviceRepository extends JpaRepository<EtaPosDevice, Long> {

    @Query("SELECT d FROM EtaPosDevice d WHERE d.pharmacy.id = :pharmacyId ORDER BY d.id")
    List<EtaPosDevice> findByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("SELECT d FROM EtaPosDevice d WHERE d.id = :id AND d.pharmacy.id = :pharmacyId")
    Optional<EtaPosDevice> findByIdAndPharmacyId(@Param("id") Long id, @Param("pharmacyId") Long pharmacyId);

    @Query("SELECT d FROM EtaPosDevice d WHERE d.pharmacy.id = :pharmacyId AND d.active = true")
    List<EtaPosDevice> findActiveByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    // Serializes receipt issuance per device: the chain head (lastReceiptUuid)
    // is read and advanced under this lock, so two concurrent sales can't both
    // chain onto the same previous receipt.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM EtaPosDevice d WHERE d.pharmacy.id = :pharmacyId AND d.active = true")
    List<EtaPosDevice> findActiveForUpdate(@Param("pharmacyId") Long pharmacyId);
}
