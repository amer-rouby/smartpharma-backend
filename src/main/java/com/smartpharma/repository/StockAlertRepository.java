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

    Page<StockAlert> findByPharmacyId(Long pharmacyId, Pageable pageable);

    Page<StockAlert> findByPharmacyIdAndStatus(Long pharmacyId, String status, Pageable pageable);

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

    // Regardless of status (including RESOLVED) - a resolved alert should not be
    // regenerated the moment the page is reopened, only after the dedup window passes.
    @Query("""
        SELECT COUNT(sa) FROM StockAlert sa
        WHERE sa.pharmacy.id = :pharmacyId
        AND sa.alertType = :alertType
        AND (:productId IS NULL OR (sa.product IS NOT NULL AND sa.product.id = :productId))
        AND (:batchId IS NULL OR (sa.batch IS NOT NULL AND sa.batch.id = :batchId))
        AND sa.createdAt > :since
    """)
    long countRecentSimilarAlerts(@Param("pharmacyId") Long pharmacyId,
                                   @Param("alertType") StockAlert.AlertType alertType,
                                   @Param("productId") Long productId,
                                   @Param("batchId") Long batchId,
                                   @Param("since") LocalDateTime since);

    void deleteByCreatedAtBefore(LocalDateTime date);

    // A product that was low/out of stock and got restocked should not keep showing
    // a stale "current stock: 0" alert forever - generateLowStockAlerts only ever
    // created alerts, it never cleared them once the underlying condition resolved.
    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        UPDATE StockAlert sa SET sa.status = 'RESOLVED', sa.resolvedAt = CURRENT_TIMESTAMP
        WHERE sa.pharmacy.id = :pharmacyId
        AND sa.product.id = :productId
        AND sa.alertType IN :alertTypes
        AND sa.status != 'RESOLVED'
    """)
    int autoResolveStockAlertsForProduct(@Param("pharmacyId") Long pharmacyId,
                                          @Param("productId") Long productId,
                                          @Param("alertTypes") List<StockAlert.AlertType> alertTypes);
}