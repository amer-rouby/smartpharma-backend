package com.smartpharma.sales.repository;

import com.smartpharma.sales.entity.SaleTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.deletedAt IS NULL
        ORDER BY st.transactionDate DESC
    """)
    Page<SaleTransaction> findByPharmacyId(Long pharmacyId, Pageable pageable);

    @Query("SELECT st FROM SaleTransaction st WHERE st.id = :id AND st.pharmacy.id = :pharmacyId AND st.deletedAt IS NULL")
    Optional<SaleTransaction> findByIdAndPharmacyId(@Param("id") Long id, @Param("pharmacyId") Long pharmacyId);

    // Row lock for the rest of the transaction: two returns on the same sale
    // can't both pass the "not more than was sold" check.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT st FROM SaleTransaction st WHERE st.id = :id AND st.pharmacy.id = :pharmacyId AND st.deletedAt IS NULL")
    Optional<SaleTransaction> lockByIdAndPharmacyId(@Param("id") Long id, @Param("pharmacyId") Long pharmacyId);

    @Query("SELECT st FROM SaleTransaction st WHERE st.pharmacy.id = :pharmacyId AND st.clientSaleId = :clientSaleId")
    Optional<SaleTransaction> findByPharmacyIdAndClientSaleId(@Param("pharmacyId") Long pharmacyId,
                                                             @Param("clientSaleId") String clientSaleId);

    List<SaleTransaction> findByPharmacyIdAndTransactionDateBetween(
            Long pharmacyId,
            LocalDateTime startDate,
            LocalDateTime endDate);

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.user.id = :userId
        AND st.transactionDate > :since
        ORDER BY st.transactionDate DESC
    """)
    List<SaleTransaction> findByPharmacyIdAndUserIdAndTransactionDateAfter(
            @Param("pharmacyId") Long pharmacyId,
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    // SaleTransaction has @Where(deleted_at IS NULL), which Hibernate applies to
    // every JPQL query against it - a native query is the only way to see
    // soft-deleted rows, which is exactly what "excessive returns" needs to count.
    @Query(value = """
        SELECT st.user_id AS userId, u.full_name AS userName, COUNT(*) AS cnt
        FROM smartpharma.sales_transactions st
        JOIN smartpharma.users u ON u.id = st.user_id
        WHERE st.pharmacy_id = :pharmacyId
        AND st.deleted_at IS NOT NULL
        AND st.deleted_at > :since
        GROUP BY st.user_id, u.full_name
    """, nativeQuery = true)
    List<Object[]> countDeletedSalesByUserSince(@Param("pharmacyId") Long pharmacyId, @Param("since") LocalDateTime since);

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
            Pageable pageable);

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND (st.invoiceNumber LIKE CONCAT('%', :query, '%') OR st.customerPhone LIKE CONCAT('%', :query, '%'))
        AND st.deletedAt IS NULL
        ORDER BY st.transactionDate DESC
    """)
    Page<SaleTransaction> searchSales(
            @Param("pharmacyId") Long pharmacyId,
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
    Long countByPharmacyIdAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT COUNT(st) FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND CAST(st.transactionDate AS date) = :date
        AND st.deletedAt IS NULL
    """)
    Long countByPharmacyIdAndDate(
            @Param("pharmacyId") Long pharmacyId,
            @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(st.totalAmount - COALESCE(st.returnedAmount, 0)), 0) FROM SaleTransaction st WHERE st.pharmacy.id = :pharmacyId AND st.deletedAt IS NULL")
    BigDecimal sumTotalAmountByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT COALESCE(SUM(st.totalAmount), 0)
               - (SELECT COALESCE(SUM(r.refundAmount), 0) FROM SaleReturn r
                 WHERE r.pharmacy.id = :pharmacyId AND r.createdAt >= :startDate AND r.createdAt <= :endDate)
        FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
    """)
    BigDecimal sumTotalAmountByPharmacyIdAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT COALESCE(SUM(st.totalAmount), 0)
               - (SELECT COALESCE(SUM(r.refundAmount), 0) FROM SaleReturn r
                  WHERE r.pharmacy.id = :pharmacyId AND CAST(r.createdAt AS date) = :date)
        FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND CAST(st.transactionDate AS date) = :date
        AND st.deletedAt IS NULL
    """)
    BigDecimal sumTotalAmountByPharmacyIdAndDate(
            @Param("pharmacyId") Long pharmacyId,
            @Param("date") LocalDate date);

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.deletedAt IS NULL
        ORDER BY st.transactionDate DESC
    """)
    List<SaleTransaction> findTop10ByPharmacyIdOrderByTransactionDateDesc(
            @Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.deletedAt IS NULL
        ORDER BY st.transactionDate DESC
    """)
    List<SaleTransaction> findRecentSalesByPharmacyId(
            @Param("pharmacyId") Long pharmacyId,
            Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(st.totalAmount), 0)
               - (SELECT COALESCE(SUM(r.refundAmount), 0) FROM SaleReturn r
                 WHERE r.pharmacy.id = :pharmacyId AND r.createdAt >= :startDate AND r.createdAt <= :endDate)
        FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
    """)
    BigDecimal getTotalRevenue(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT COUNT(st) FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
    """)
    Long getTotalOrders(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT st.paymentMethod, COALESCE(SUM(st.totalAmount), 0)
               - (SELECT COALESCE(SUM(r.refundAmount), 0) FROM SaleReturn r
                  WHERE r.pharmacy.id = :pharmacyId AND r.saleTransaction.paymentMethod = st.paymentMethod
                  AND r.createdAt >= :startDate AND r.createdAt <= :endDate)
        FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
        GROUP BY st.paymentMethod
    """)
    List<Object[]> getRevenueByPaymentMethod(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT si.product.id, si.product.name,
               SUM(si.quantity) - COALESCE((SELECT SUM(ri.quantity) FROM SaleReturnItem ri
                   WHERE ri.saleItem.product.id = si.product.id
                   AND ri.saleItem.transaction.pharmacy.id = :pharmacyId
                   AND ri.saleItem.transaction.deletedAt IS NULL
                   AND ri.saleItem.transaction.transactionDate >= :startDate
                   AND ri.saleItem.transaction.transactionDate <= :endDate), 0) AS netQuantity,
               SUM(si.totalPrice) - COALESCE((SELECT SUM(ri.totalPrice) FROM SaleReturnItem ri
                   WHERE ri.saleItem.product.id = si.product.id
                   AND ri.saleItem.transaction.pharmacy.id = :pharmacyId
                   AND ri.saleItem.transaction.deletedAt IS NULL
                   AND ri.saleItem.transaction.transactionDate >= :startDate
                   AND ri.saleItem.transaction.transactionDate <= :endDate), 0)
        FROM SaleItem si
        JOIN SaleTransaction st ON si.transaction.id = st.id
        WHERE st.pharmacy.id = :pharmacyId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
        GROUP BY si.product.id, si.product.name
        ORDER BY netQuantity DESC
    """)
    List<Object[]> getTopProducts(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("""
        SELECT CAST(st.transactionDate AS date), COALESCE(SUM(st.totalAmount), 0), COUNT(st)
        FROM SaleTransaction st
        WHERE st.pharmacy.id = :pharmacyId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
        GROUP BY CAST(st.transactionDate AS date)
        ORDER BY CAST(st.transactionDate AS date)
    """)
    List<Object[]> getDailySalesOnly(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // [day, refunded] - refunds counted on the day of the return.
    @Query("""
        SELECT CAST(r.createdAt AS date), COALESCE(SUM(r.refundAmount), 0)
        FROM SaleReturn r
        WHERE r.pharmacy.id = :pharmacyId
        AND r.createdAt >= :startDate
        AND r.createdAt <= :endDate
        GROUP BY CAST(r.createdAt AS date)
    """)
    List<Object[]> getDailyRefunds(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT si.product.id, si.product.name,
               SUM(si.quantity) - COALESCE((SELECT SUM(ri.quantity) FROM SaleReturnItem ri
                   WHERE ri.saleItem.product.id = si.product.id
                   AND ri.saleItem.transaction.pharmacy.id = :pharmacyId
                   AND ri.saleItem.transaction.deletedAt IS NULL), 0) AS netQuantity,
               SUM(si.totalPrice) - COALESCE((SELECT SUM(ri.totalPrice) FROM SaleReturnItem ri
                   WHERE ri.saleItem.product.id = si.product.id
                   AND ri.saleItem.transaction.pharmacy.id = :pharmacyId
                   AND ri.saleItem.transaction.deletedAt IS NULL), 0)
        FROM SaleItem si
        WHERE si.transaction.pharmacy.id = :pharmacyId
        AND si.transaction.deletedAt IS NULL
        GROUP BY si.product.id, si.product.name
        ORDER BY netQuantity DESC
    """)
    List<Object[]> findTopSellingProducts(
            @Param("pharmacyId") Long pharmacyId,
            Pageable pageable);
}