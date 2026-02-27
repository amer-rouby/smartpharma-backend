package com.smartpharma.repository;

import com.smartpharma.entity.StockMovement;
import com.smartpharma.entity.StockMovement.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByBatchId(Long batchId, Pageable pageable);

    Page<StockMovement> findByPharmacyId(Long pharmacyId, Pageable pageable);

    @Query("""
        SELECT sm FROM StockMovement sm
        WHERE sm.pharmacyId = :pharmacyId
        AND sm.movementType = :type
        ORDER BY sm.movementDate DESC
    """)
    Page<StockMovement> findByPharmacyIdAndType(
            @Param("pharmacyId") Long pharmacyId,
            @Param("type") MovementType type,
            Pageable pageable
    );

    @Query("""
        SELECT sm FROM StockMovement sm
        WHERE sm.pharmacyId = :pharmacyId
        AND sm.movementDate BETWEEN :startDate AND :endDate
        ORDER BY sm.movementDate DESC
    """)
    Page<StockMovement> findByPharmacyIdAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
        SELECT sm FROM StockMovement sm
        WHERE sm.batch.id = :batchId
        ORDER BY sm.movementDate DESC
    """)
    List<StockMovement> findByBatchIdOrdered(@Param("batchId") Long batchId);

    @Query("""
        SELECT COUNT(sm) FROM StockMovement sm
        WHERE sm.pharmacyId = :pharmacyId
        AND sm.movementType = :type
        AND sm.movementDate BETWEEN :startDate AND :endDate
    """)
    Long countMovementsByTypeAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("type") MovementType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT COALESCE(SUM(sm.quantity), 0) FROM StockMovement sm
        WHERE sm.pharmacyId = :pharmacyId
        AND sm.movementType = 'STOCK_IN'
        AND sm.movementDate BETWEEN :startDate AND :endDate
    """)
    Integer sumStockInByDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT COALESCE(SUM(sm.quantity), 0) FROM StockMovement sm
        WHERE sm.pharmacyId = :pharmacyId
        AND sm.movementType = 'STOCK_OUT'
        AND sm.movementDate BETWEEN :startDate AND :endDate
    """)
    Integer sumStockOutByDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}