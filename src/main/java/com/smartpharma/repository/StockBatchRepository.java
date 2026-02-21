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

    List<StockBatch> findByProductIdAndStatus(Long productId, BatchStatus status);

    List<StockBatch> findByPharmacyIdAndStatus(Long pharmacyId, BatchStatus status);

    // ✅ ✅ ✅ إضافة method للبحث بـ ACTIVE status فقط ✅ ✅ ✅
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

    @Lock(jakarta.persistence.LockModeType.OPTIMISTIC)
    StockBatch findByIdAndStatus(Long id, BatchStatus status);

    // ✅ ✅ ✅ إضافة method لحساب إجمالي المخزون ✅ ✅ ✅
    @Query("""
        SELECT COALESCE(SUM(sb.quantityCurrent), 0) 
        FROM StockBatch sb 
        WHERE sb.product.id = :productId 
        AND sb.status = 'ACTIVE'
    """)
    Long sumQuantityByProductId(@Param("productId") Long productId);

    // ✅ ✅ ✅ إضافة method لتحديث المخزون ✅ ✅ ✅
    @Query("""
        UPDATE StockBatch sb 
        SET sb.quantityCurrent = sb.quantityCurrent - :deduct 
        WHERE sb.id = :batchId 
        AND sb.quantityCurrent >= :deduct
    """)
    int deductQuantity(@Param("batchId") Long batchId, @Param("deduct") Integer deduct);
}