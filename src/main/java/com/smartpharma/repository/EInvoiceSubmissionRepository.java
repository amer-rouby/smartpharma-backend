package com.smartpharma.repository;

import com.smartpharma.entity.EInvoiceSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EInvoiceSubmissionRepository extends JpaRepository<EInvoiceSubmission, Long> {

    @Query("""
        SELECT e FROM EInvoiceSubmission e
        WHERE e.saleTransaction.id = :saleTransactionId
    """)
    Optional<EInvoiceSubmission> findBySaleTransactionId(@Param("saleTransactionId") Long saleTransactionId);
}
