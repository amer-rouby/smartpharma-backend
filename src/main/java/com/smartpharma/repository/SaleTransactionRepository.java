package com.smartpharma.repository;

import com.smartpharma.entity.SaleTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleTransactionRepository extends JpaRepository<SaleTransaction, Long> {

    Page<SaleTransaction> findByPharmacyId(Long pharmacyId, Pageable pageable);

    @Query("SELECT st FROM SaleTransaction st WHERE st.id = :id AND st.pharmacy.id = :pharmacyId AND st.deletedAt IS NULL")
    Optional<SaleTransaction> findByIdAndPharmacyId(@Param("id") Long id, @Param("pharmacyId") Long pharmacyId);

    List<SaleTransaction> findByPharmacyIdAndTransactionDateBetween(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
        ORDER BY st.transactionDate DESC
    """)
    Page<SaleTransaction> findByPharmacyIdAndDateRange(@Param("pharmacyId") Long pharmacyId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate,
                                                       Pageable pageable);

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND (st.invoiceNumber LIKE CONCAT('%', :query, '%') OR st.customerPhone LIKE CONCAT('%', :query, '%'))
        AND st.deletedAt IS NULL
        ORDER BY st.transactionDate DESC
    """)
    Page<SaleTransaction> searchSales(@Param("pharmacyId") Long pharmacyId,
                                      @Param("query") String query,
                                      Pageable pageable);

    @Query("SELECT COUNT(st) FROM SaleTransaction st WHERE st.pharmacy.id = :pharmacyId AND st.deletedAt IS NULL")
    Long countByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT COUNT(st) FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
    """)
    Long countByPharmacyIdAndDateRange(@Param("pharmacyId") Long pharmacyId,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT COUNT(st) FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND CAST(st.transactionDate AS date) = :date
        AND st.deletedAt IS NULL
    """)
    Long countByPharmacyIdAndDate(@Param("pharmacyId") Long pharmacyId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(st.totalAmount), 0) FROM SaleTransaction st WHERE st.pharmacy.id = :pharmacyId AND st.deletedAt IS NULL")
    BigDecimal sumTotalAmountByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT COALESCE(SUM(st.totalAmount), 0) FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
    """)
    BigDecimal sumTotalAmountByPharmacyIdAndDateRange(@Param("pharmacyId") Long pharmacyId,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT COALESCE(SUM(st.totalAmount), 0) FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND CAST(st.transactionDate AS date) = :date
        AND st.deletedAt IS NULL
    """)
    BigDecimal sumTotalAmountByPharmacyIdAndDate(@Param("pharmacyId") Long pharmacyId, @Param("date") LocalDate date);

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.deletedAt IS NULL
        ORDER BY st.transactionDate DESC
    """)
    List<SaleTransaction> findTop10ByPharmacyIdOrderByTransactionDateDesc(@Param("pharmacyId") Long pharmacyId);
}