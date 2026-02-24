package com.smartpharma.repository;

import com.smartpharma.entity.StockBatch;
import com.smartpharma.entity.StockBatch.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    // ================================
    // ✅ Basic Queries
    // ================================

    List<StockBatch> findByProductIdAndStatus(Long productId, BatchStatus status);

    List<StockBatch> findByPharmacyIdAndStatus(Long pharmacyId, BatchStatus status);

    default List<StockBatch> findByProductIdAndStatusActive(Long productId) {
        return findByProductIdAndStatus(productId, BatchStatus.ACTIVE);
    }

    @Query("""
        SELECT sb FROM StockBatch sb 
        WHERE sb.product.id = :productId 
        AND sb.status = 'ACTIVE' 
        AND sb.quantityCurrent > 0
        ORDER BY sb.expiryDate ASC
    """)
    List<StockBatch> findActiveBatchesByProductOrderedByExpiry(@Param("productId") Long productId);

    @Query("""
        SELECT sb FROM StockBatch sb 
        WHERE sb.pharmacy.id = :pharmacyId 
        AND sb.status = 'ACTIVE' 
        AND sb.expiryDate <= :expiryDate
        ORDER BY sb.expiryDate ASC
    """)
    List<StockBatch> findExpiringBatches(@Param("pharmacyId") Long pharmacyId, @Param("expiryDate") LocalDate expiryDate);

    // ✅ FIXED: Added missing method to find expired batches (return List<StockBatch>)
    @Query("""
        SELECT sb FROM StockBatch sb 
        WHERE sb.pharmacy.id = :pharmacyId 
        AND sb.status = 'ACTIVE' 
        AND sb.expiryDate < :expiryDate
        ORDER BY sb.expiryDate ASC
    """)
    List<StockBatch> findExpiredBatches(@Param("pharmacyId") Long pharmacyId, @Param("expiryDate") LocalDate expiryDate);

    @Lock(jakarta.persistence.LockModeType.OPTIMISTIC)
    StockBatch findByIdAndStatus(Long id, BatchStatus status);

    @Query("""
        SELECT COALESCE(SUM(sb.quantityCurrent), 0) 
        FROM StockBatch sb 
        WHERE sb.product.id = :productId 
        AND sb.status = 'ACTIVE'
    """)
    Long sumQuantityByProductId(@Param("productId") Long productId);

    @Query("""
        UPDATE StockBatch sb 
        SET sb.quantityCurrent = sb.quantityCurrent - :deduct 
        WHERE sb.id = :batchId 
        AND sb.quantityCurrent >= :deduct
    """)
    int deductQuantity(@Param("batchId") Long batchId, @Param("deduct") Integer deduct);

    @Query("""
        SELECT COUNT(sb) FROM StockBatch sb
        WHERE sb.pharmacy.id = :pharmacyId
        AND sb.status = 'ACTIVE'
        AND sb.expiryDate BETWEEN CURRENT_DATE AND :expiryDate
    """)
    Long countExpiringBatches(@Param("pharmacyId") Long pharmacyId, @Param("expiryDate") LocalDate expiryDate);

    @Query("""
        SELECT COUNT(sb) FROM StockBatch sb
        WHERE sb.pharmacy.id = :pharmacyId
        AND sb.status = 'ACTIVE'
        AND sb.expiryDate < CURRENT_DATE
    """)
    Long countExpiredBatches(@Param("pharmacyId") Long pharmacyId);

    // ================================
    // ✅ Report Queries (PostgreSQL compatible)
    // ================================

    @Query("""
        SELECT COALESCE(SUM(sb.quantityCurrent * p.sellPrice), 0)
        FROM StockBatch sb
        JOIN Product p ON sb.product.id = p.id
        WHERE sb.pharmacy.id = :pharmacyId
        AND sb.status = 'ACTIVE'
    """)
    BigDecimal getTotalStockValue(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT p.category, COUNT(DISTINCT p.id), COALESCE(SUM(sb.quantityCurrent * p.sellPrice), 0)
        FROM StockBatch sb
        JOIN Product p ON sb.product.id = p.id
        WHERE sb.pharmacy.id = :pharmacyId
        AND sb.status = 'ACTIVE'
        GROUP BY p.category
    """)
    List<Object[]> getStockByCategory(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT p.id, p.name, sb.batchNumber, SUM(sb.quantityCurrent), p.minStockLevel, sb.expiryDate
        FROM StockBatch sb
        JOIN Product p ON sb.product.id = p.id
        WHERE sb.pharmacy.id = :pharmacyId
        AND sb.status = 'ACTIVE'
        GROUP BY p.id, p.name, sb.batchNumber, p.minStockLevel, sb.expiryDate
        HAVING SUM(sb.quantityCurrent) <= p.minStockLevel
        ORDER BY SUM(sb.quantityCurrent) ASC
    """)
    List<Object[]> getLowStockProducts(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT p.id, p.name, sb.batchNumber, sb.expiryDate, SUM(sb.quantityCurrent)
        FROM StockBatch sb
        JOIN Product p ON sb.product.id = p.id
        WHERE sb.pharmacy.id = :pharmacyId
        AND sb.status = 'ACTIVE'
        AND sb.expiryDate >= CURRENT_DATE
        AND sb.expiryDate <= :expiryDate
        GROUP BY p.id, p.name, sb.batchNumber, sb.expiryDate
        ORDER BY sb.expiryDate ASC
    """)
    List<Object[]> getExpiringProducts(@Param("pharmacyId") Long pharmacyId,
                                       @Param("expiryDate") LocalDate expiryDate);

    @Query("""
        SELECT COALESCE(SUM(sb.quantityCurrent), 0)
        FROM StockBatch sb
        WHERE sb.pharmacy.id = :pharmacyId
        AND sb.status = 'ACTIVE'
    """)
    Long getTotalStockItemsCount(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT COUNT(DISTINCT p.id)
        FROM StockBatch sb
        JOIN Product p ON sb.product.id = p.id
        WHERE sb.pharmacy.id = :pharmacyId
        AND sb.status = 'ACTIVE'
        GROUP BY p.id, p.minStockLevel
        HAVING SUM(sb.quantityCurrent) <= p.minStockLevel
    """)
    Long countLowStockItems(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT p.id, p.name, sb.batchNumber, sb.expiryDate, sb.quantityCurrent
        FROM StockBatch sb
        JOIN Product p ON sb.product.id = p.id
        WHERE sb.pharmacy.id = :pharmacyId
        AND sb.status = 'ACTIVE'
        AND sb.expiryDate >= :startDate
        AND sb.expiryDate <= :endDate
    """)
    List<Object[]> getExpiringBatchesInRange(@Param("pharmacyId") Long pharmacyId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
}