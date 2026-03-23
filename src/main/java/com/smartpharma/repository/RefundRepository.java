package com.smartpharma.repository;

import com.smartpharma.entity.Refund;
import com.smartpharma.entity.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByPaymentId(Long paymentId);

    Page<Refund> findByStatus(RefundStatus status, Pageable pageable);

    @Query("SELECT r FROM Refund r WHERE r.payment.pharmacy.id = :pharmacyId ORDER BY r.createdAt DESC")
    Page<Refund> findByPharmacyId(@Param("pharmacyId") Long pharmacyId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Refund r WHERE r.payment.pharmacy.id = :pharmacyId AND r.status = :status")
    Long countByPharmacyIdAndStatus(@Param("pharmacyId") Long pharmacyId, @Param("status") RefundStatus status);

    @Query("SELECT SUM(r.refundAmount) FROM Refund r WHERE r.payment.pharmacy.id = :pharmacyId AND r.status = 'COMPLETED'")
    java.math.BigDecimal getTotalRefundedAmount(@Param("pharmacyId") Long pharmacyId);
}