package com.smartpharma.repository;

import com.smartpharma.entity.StockAdjustmentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}