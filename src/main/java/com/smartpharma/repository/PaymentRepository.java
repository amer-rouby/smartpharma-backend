package com.smartpharma.repository;

import com.smartpharma.entity.Payment;
import com.smartpharma.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReferenceNumber(String referenceNumber);

    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);

    Page<Payment> findByPharmacyId(Long pharmacyId, Pageable pageable);

    Page<Payment> findByPharmacyIdAndStatus(Long pharmacyId, PaymentStatus status, Pageable pageable);

    List<Payment> findByPharmacyIdAndStatus(Long pharmacyId, PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.pharmacy.id = :pharmacyId " +
            "AND p.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY p.createdAt DESC")
    Page<Payment> findByPharmacyIdAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.pharmacy.id = :pharmacyId AND p.status = :status")
    Long countByPharmacyIdAndStatus(@Param("pharmacyId") Long pharmacyId, @Param("status") PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.pharmacy.id = :pharmacyId AND p.status = 'COMPLETED'")
    BigDecimal getTotalCompletedPayments(@Param("pharmacyId") Long pharmacyId);
}