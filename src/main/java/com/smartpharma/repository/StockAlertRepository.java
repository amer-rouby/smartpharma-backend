package com.smartpharma.repository;

import com.smartpharma.entity.StockAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    // ================================
    // ✅ Pagination Queries
    // ================================
    Page<StockAlert> findByPharmacyId(Long pharmacyId, Pageable pageable);

    Page<StockAlert> findByPharmacyIdAndStatus(Long pharmacyId, String status, Pageable pageable);

    // ================================
    // ✅ Count Queries - FIXED: Added missing methods
    // ================================

    // ✅ هذا اللي كان ناقص!
    long countByPharmacyId(Long pharmacyId);

    @Query("""
        SELECT COUNT(sa) FROM StockAlert sa
        WHERE sa.pharmacy.id = :pharmacyId
        AND sa.status = 'UNREAD'
    """)
    Long countUnreadAlerts(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT COUNT(sa) FROM StockAlert sa
        WHERE sa.pharmacy.id = :pharmacyId
        AND sa.alertType = :alertType
        AND sa.status != 'RESOLVED'
    """)
    Long countActiveAlertsByType(@Param("pharmacyId") Long pharmacyId,
                                 @Param("alertType") StockAlert.AlertType alertType);

    // ================================
    // ✅ List Queries
    // ================================
    List<StockAlert> findByPharmacyIdAndStatusAndCreatedAtAfter(
            Long pharmacyId,
            String status,
            LocalDateTime since
    );

    @Query("""
        SELECT sa FROM StockAlert sa
        WHERE sa.pharmacy.id = :pharmacyId
        AND sa.status != 'RESOLVED'
        ORDER BY sa.createdAt DESC
    """)
    List<StockAlert> findActiveAlerts(@Param("pharmacyId") Long pharmacyId);

    // ================================
    // ✅ Delete Query
    // ================================
    void deleteByCreatedAtBefore(LocalDateTime date);
}