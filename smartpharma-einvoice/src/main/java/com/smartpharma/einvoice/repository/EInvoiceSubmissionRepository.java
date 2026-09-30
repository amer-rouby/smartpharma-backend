package com.smartpharma.einvoice.repository;

import com.smartpharma.einvoice.entity.EInvoiceSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EInvoiceSubmissionRepository extends JpaRepository<EInvoiceSubmission, Long> {

    @Query("""
        SELECT e FROM EInvoiceSubmission e
        WHERE e.saleTransaction.id = :saleTransactionId
    """)
    Optional<EInvoiceSubmission> findBySaleTransactionId(@Param("saleTransactionId") Long saleTransactionId);

    // Issued receipts still waiting to reach ETA, oldest first so a batch
    // follows the device's chain order.
    @Query("""
        SELECT e FROM EInvoiceSubmission e
        WHERE e.posDevice.id = :deviceId
          AND e.status IN :statuses
          AND e.receiptJson IS NOT NULL
          AND e.retryCount < :maxAttempts
        ORDER BY e.id
    """)
    List<EInvoiceSubmission> findDeliverable(@Param("deviceId") Long deviceId,
                                             @Param("statuses") Collection<EInvoiceSubmission.Status> statuses,
                                             @Param("maxAttempts") int maxAttempts);

    @Query("""
        SELECT DISTINCT e.posDevice.id FROM EInvoiceSubmission e
        WHERE e.status IN :statuses
          AND e.receiptJson IS NOT NULL
          AND e.retryCount < :maxAttempts
    """)
    List<Long> findDeviceIdsWithDeliverable(@Param("statuses") Collection<EInvoiceSubmission.Status> statuses,
                                            @Param("maxAttempts") int maxAttempts);
}
