package com.smartpharma.repository;

import com.smartpharma.entity.StockAdjustmentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockAdjustmentHistoryRepository extends JpaRepository<StockAdjustmentHistory, Long> {

    List<StockAdjustmentHistory> findByBatchIdOrderByAdjustmentDateDesc(Long batchId);

    Page<StockAdjustmentHistory> findByBatchIdOrderByAdjustmentDateDesc(Long batchId, Pageable pageable);

    @Query("""
        SELECT h FROM StockAdjustmentHistory h
        WHERE h.batch.pharmacy.id = :pharmacyId
        ORDER BY h.adjustmentDate DESC
    """)
    Page<StockAdjustmentHistory> findByPharmacyId(@Param("pharmacyId") Long pharmacyId, Pageable pageable);

    @Query("""
        SELECT h FROM StockAdjustmentHistory h
        WHERE h.batch.product.id = :productId
        AND h.batch.pharmacy.id = :pharmacyId
        ORDER BY h.adjustmentDate DESC
    """)
    List<StockAdjustmentHistory> findByProductIdAndPharmacyId(
            @Param("productId") Long productId,
            @Param("pharmacyId") Long pharmacyId);

    // adjustedByName is often left unset by callers, so the display name is
    // resolved from the real User record via adjustedBy (a plain id column,
    // not a JPA relation) instead of trusting that denormalized field.
    @Query("""
        SELECT h.adjustedBy AS userId, u.fullName AS userName, COUNT(h) AS cnt
        FROM StockAdjustmentHistory h, User u
        WHERE h.batch.pharmacy.id = :pharmacyId
        AND h.adjustmentDate > :since
        AND h.adjustedBy IS NOT NULL
        AND u.id = h.adjustedBy
        GROUP BY h.adjustedBy, u.fullName
    """)
    List<Object[]> countAdjustmentsByUserSince(@Param("pharmacyId") Long pharmacyId, @Param("since") LocalDateTime since);

    @Query("""
        SELECT h FROM StockAdjustmentHistory h
        WHERE h.batch.pharmacy.id = :pharmacyId
        AND h.adjustmentDate > :since
        AND ABS(h.newQuantity - h.previousQuantity) >= :minDelta
        ORDER BY h.adjustmentDate DESC
    """)
    List<StockAdjustmentHistory> findLargeDiscrepanciesSince(
            @Param("pharmacyId") Long pharmacyId,
            @Param("since") LocalDateTime since,
            @Param("minDelta") int minDelta);
}