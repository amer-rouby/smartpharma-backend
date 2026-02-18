package com.smartpharma.repository;

import com.smartpharma.entity.SaleTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SaleTransactionRepository extends JpaRepository<SaleTransaction, Long> {

    Page<SaleTransaction> findByPharmacyId(Long pharmacyId, Pageable pageable);

    @Query("""
        SELECT COUNT(st) FROM SaleTransaction st 
        WHERE st.pharmacy.id = :pharmacyId 
        AND st.deletedAt IS NULL
    """)
    Long countByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT COALESCE(SUM(st.totalAmount), 0) FROM SaleTransaction st 
        WHERE st.pharmacy.id = :pharmacyId 
        AND st.deletedAt IS NULL
    """)
    java.math.BigDecimal sumTotalAmountByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT COUNT(st) FROM SaleTransaction st 
        WHERE st.pharmacy.id = :pharmacyId 
        AND st.transactionDate >= :startDate 
        AND st.transactionDate <= :endDate 
        AND st.deletedAt IS NULL
    """)
    Long countByPharmacyIdAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT COALESCE(SUM(st.totalAmount), 0) FROM SaleTransaction st 
        WHERE st.pharmacy.id = :pharmacyId 
        AND st.transactionDate >= :startDate 
        AND st.transactionDate <= :endDate 
        AND st.deletedAt IS NULL
    """)
    java.math.BigDecimal sumTotalAmountByPharmacyIdAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT st FROM SaleTransaction st 
        WHERE st.pharmacy.id = :pharmacyId 
        AND st.transactionDate >= :startDate 
        AND st.transactionDate <= :endDate 
        AND st.deletedAt IS NULL 
        ORDER BY st.transactionDate DESC
    """)
    Page<SaleTransaction> findByPharmacyIdAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
        SELECT st FROM SaleTransaction st 
        WHERE st.pharmacy.id = :pharmacyId 
        AND (st.invoiceNumber LIKE %:query% OR st.customerPhone LIKE %:query%) 
        AND st.deletedAt IS NULL 
        ORDER BY st.transactionDate DESC
    """)
    Page<SaleTransaction> searchSales(
            @Param("pharmacyId") Long pharmacyId,
            @Param("query") String query,
            Pageable pageable
    );
}
