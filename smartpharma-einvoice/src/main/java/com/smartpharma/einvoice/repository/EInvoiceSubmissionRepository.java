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
          AND e.documentType = :documentType
    """)
    Optional<EInvoiceSubmission> findBySaleAndType(@Param("saleTransactionId") Long saleTransactionId,
                                                   @Param("documentType") EInvoiceSubmission.DocumentType documentType);

    // The sales receipt of a sale (a cancelled sale also has a RETURN row).
    default Optional<EInvoiceSubmission> findBySaleTransactionId(Long saleTransactionId) {
        return findBySaleAndType(saleTransactionId, EInvoiceSubmission.DocumentType.SALE);
    }

    @Query("SELECT e FROM EInvoiceSubmission e WHERE e.originalSubmission.id = :originalId")
    Optional<EInvoiceSubmission> findReturnOf(@Param("originalId") Long originalId);

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

    // Submissions ETA accepted for processing whose receipts' final validity
    // (Valid/Invalid) hasn't been read back yet: [deviceId, submissionUuid].
    @Query("""
        SELECT DISTINCT e.posDevice.id, e.submissionUuid FROM EInvoiceSubmission e
        WHERE e.status = :status
          AND e.submissionUuid IS NOT NULL
    """)
    List<Object[]> findDeviceSubmissionPairs(@Param("status") EInvoiceSubmission.Status status);

    default List<Object[]> findSubmissionsAwaitingValidation() {
        return findDeviceSubmissionPairs(EInvoiceSubmission.Status.SUBMITTED);
    }

    @Query("""
        SELECT e FROM EInvoiceSubmission e
        WHERE e.submissionUuid = :submissionUuid
          AND e.status = :status
    """)
    List<EInvoiceSubmission> findBySubmissionUuidAndStatus(@Param("submissionUuid") String submissionUuid,
                                                          @Param("status") EInvoiceSubmission.Status status);

    default List<EInvoiceSubmission> findSubmittedBySubmissionUuid(String submissionUuid) {
        return findBySubmissionUuidAndStatus(submissionUuid, EInvoiceSubmission.Status.SUBMITTED);
    }

    @Query("""
        SELECT DISTINCT e.posDevice.id FROM EInvoiceSubmission e
        WHERE e.status IN :statuses
          AND e.receiptJson IS NOT NULL
          AND e.retryCount < :maxAttempts
    """)
    List<Long> findDeviceIdsWithDeliverable(@Param("statuses") Collection<EInvoiceSubmission.Status> statuses,
                                            @Param("maxAttempts") int maxAttempts);
}
