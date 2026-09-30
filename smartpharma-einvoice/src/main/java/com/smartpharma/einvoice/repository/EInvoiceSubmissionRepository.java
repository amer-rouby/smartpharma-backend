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

    // The return receipt reversing the whole original (the sale was cancelled).
    @Query("SELECT e FROM EInvoiceSubmission e WHERE e.originalSubmission.id = :originalId AND e.saleReturn IS NULL")
    Optional<EInvoiceSubmission> findReturnOf(@Param("originalId") Long originalId);

    @Query("""
        SELECT e FROM EInvoiceSubmission e
        WHERE e.saleTransaction.id = :saleTransactionId
          AND e.documentType = :documentType
        ORDER BY e.id
    """)
    List<EInvoiceSubmission> findAllBySaleAndType(@Param("saleTransactionId") Long saleTransactionId,
                                                  @Param("documentType") EInvoiceSubmission.DocumentType documentType);

    default List<EInvoiceSubmission> findReturnsBySaleTransactionId(Long saleTransactionId) {
        return findAllBySaleAndType(saleTransactionId, EInvoiceSubmission.DocumentType.RETURN);
    }

    @Query("SELECT e FROM EInvoiceSubmission e WHERE e.saleReturn.id = :saleReturnId")
    Optional<EInvoiceSubmission> findBySaleReturnId(@Param("saleReturnId") Long saleReturnId);

    // Receipts of a pharmacy that need someone to act: ETA rejected them or
    // they couldn't be issued/delivered - including returns of cancelled
    // sales, which the sales screens no longer show. A cancelled sale's own
    // receipt is left out: its return receipt is what matters then. Native, so
    // the soft-delete filter on sales doesn't hide the cancelled ones:
    // [submissionId, invoiceNumber, saleCancelled].
    @Query(value = """
        SELECT e.id, s.invoice_number, (s.deleted_at IS NOT NULL)
        FROM smartpharma.einvoice_submissions e
        JOIN smartpharma.sales_transactions s ON s.id = e.sale_transaction_id
        WHERE s.pharmacy_id = :pharmacyId
          AND e.status IN ('REJECTED', 'ERROR')
          AND NOT (COALESCE(e.document_type, 'SALE') = 'SALE' AND s.deleted_at IS NOT NULL)
        ORDER BY e.id DESC
        LIMIT 200
    """, nativeQuery = true)
    List<Object[]> findNeedingAttention(@Param("pharmacyId") Long pharmacyId);

    // [pharmacyId] of the sale a receipt belongs to, cancelled or not.
    @Query(value = """
        SELECT s.pharmacy_id FROM smartpharma.einvoice_submissions e
        JOIN smartpharma.sales_transactions s ON s.id = e.sale_transaction_id
        WHERE e.id = :submissionId
    """, nativeQuery = true)
    Optional<Long> findPharmacyIdOf(@Param("submissionId") Long submissionId);

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
