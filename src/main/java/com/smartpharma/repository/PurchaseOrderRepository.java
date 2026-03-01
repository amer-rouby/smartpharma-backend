package com.smartpharma.repository;

import com.smartpharma.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("""
        SELECT po FROM PurchaseOrder po 
        WHERE po.pharmacy.id = :pharmacyId 
        AND po.deletedAt IS NULL 
        ORDER BY po.createdAt DESC
    """)
    Page<PurchaseOrder> findByPharmacyId(@Param("pharmacyId") Long pharmacyId, Pageable pageable);

    @Query("""
        SELECT po FROM PurchaseOrder po 
        WHERE po.pharmacy.id = :pharmacyId 
        AND po.deletedAt IS NULL
        AND po.status = :status
        ORDER BY po.createdAt DESC
    """)
    Page<PurchaseOrder> findByPharmacyIdAndStatus(@Param("pharmacyId") Long pharmacyId,
                                                  @Param("status") String status,
                                                  Pageable pageable);

    Optional<PurchaseOrder> findByIdAndPharmacyIdAndDeletedAtIsNull(Long id, Long pharmacyId);

    @Query("""
        SELECT COUNT(po) FROM PurchaseOrder po 
        WHERE po.pharmacy.id = :pharmacyId 
        AND po.deletedAt IS NULL
    """)
    Long countByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT COUNT(po) FROM PurchaseOrder po 
        WHERE po.pharmacy.id = :pharmacyId 
        AND po.deletedAt IS NULL 
        AND po.status = :status
    """)
    Long countByPharmacyIdAndStatus(@Param("pharmacyId") Long pharmacyId, @Param("status") String status);

    @Query("""
        SELECT po FROM PurchaseOrder po 
        WHERE po.pharmacy.id = :pharmacyId 
        AND po.deletedAt IS NULL
        AND po.orderDate BETWEEN :startDate AND :endDate
        ORDER BY po.orderDate DESC
    """)
    List<PurchaseOrder> findByPharmacyIdAndDateRange(@Param("pharmacyId") Long pharmacyId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT SUM(po.totalAmount) FROM PurchaseOrder po 
        WHERE po.pharmacy.id = :pharmacyId 
        AND po.deletedAt IS NULL 
        AND po.status = 'RECEIVED'
        AND po.orderDate BETWEEN :startDate AND :endDate
    """)
    java.math.BigDecimal sumTotalAmountByPharmacyIdAndDateRange(@Param("pharmacyId") Long pharmacyId,
                                                                @Param("startDate") LocalDate startDate,
                                                                @Param("endDate") LocalDate endDate);

    boolean existsByPharmacyIdAndOrderNumberAndDeletedAtIsNull(Long pharmacyId, String orderNumber);

    @Query("""
        SELECT po FROM PurchaseOrder po 
        WHERE po.sourceType = 'PREDICTION' 
        AND po.sourceId = :predictionId 
        AND po.deletedAt IS NULL
    """)
    List<PurchaseOrder> findByPredictionId(@Param("predictionId") Long predictionId);
}