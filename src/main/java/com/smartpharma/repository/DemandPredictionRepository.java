package com.smartpharma.repository;

import com.smartpharma.entity.DemandPrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DemandPrediction entity queries.
 * Handles all database operations for demand forecasting.
 */
@Repository
public interface DemandPredictionRepository extends JpaRepository<DemandPrediction, Long> {

    /**
     * Find predictions for a specific product and pharmacy.
     * Results ordered by prediction date (newest first).
     */
    List<DemandPrediction> findByProductIdAndPharmacyIdOrderByPredictionDateDesc(
            Long productId, Long pharmacyId);

    /**
     * Find future predictions (not yet realized) for a pharmacy.
     * Used for dashboard upcoming predictions view.
     */
    @Query("""
        SELECT dp FROM DemandPrediction dp
        WHERE dp.pharmacy.id = :pharmacyId
        AND dp.predictionDate >= :today
        ORDER BY dp.predictionDate ASC
    """)
    List<DemandPrediction> findFuturePredictions(
            @Param("pharmacyId") Long pharmacyId,
            @Param("today") LocalDate today);

    /**
     * Paginated query for all predictions in a pharmacy.
     * Used for list views with pagination support.
     */
    Page<DemandPrediction> findByPharmacyId(Long pharmacyId, Pageable pageable);

    /**
     * Find unique prediction by product, pharmacy, and date.
     * Used for upsert logic (update if exists, else insert).
     */
    Optional<DemandPrediction> findByProductIdAndPharmacyIdAndPredictionDate(
            Long productId, Long pharmacyId, LocalDate predictionDate);

    /**
     * Get upcoming predictions within date range (not yet realized).
     * Filters out predictions that already have actual sales data.
     */
    @Query("""
        SELECT dp FROM DemandPrediction dp
        WHERE dp.pharmacy.id = :pharmacyId
        AND dp.predictionDate >= :startDate
        AND dp.predictionDate <= :endDate
        AND dp.actualQuantity IS NULL
        ORDER BY dp.predictionDate ASC
    """)
    List<DemandPrediction> findUpcomingPredictions(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count total predictions for a pharmacy.
     * Used for accuracy statistics dashboard.
     */
    @Query("SELECT COUNT(dp) FROM DemandPrediction dp WHERE dp.pharmacy.id = :pharmacyId")
    Long countByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    /**
     * Calculate average accuracy for realized predictions.
     * Only includes predictions with actual sales data.
     */
    @Query("""
        SELECT AVG(dp.accuracyPercentage) FROM DemandPrediction dp
        WHERE dp.pharmacy.id = :pharmacyId
        AND dp.actualQuantity IS NOT NULL
        AND dp.accuracyPercentage IS NOT NULL
    """)
    BigDecimal calculateAverageAccuracyByPharmacy(@Param("pharmacyId") Long pharmacyId);

    /**
     * Get the latest prediction date for a pharmacy.
     * Used to display "last updated" timestamp in UI.
     */
    @Query("SELECT MAX(dp.predictionDate) FROM DemandPrediction dp WHERE dp.pharmacy.id = :pharmacyId")
    Optional<LocalDate> findLatestPredictionDateByPharmacy(@Param("pharmacyId") Long pharmacyId);

    /**
     * Delete old predictions before specified date.
     * Used for cleanup/maintenance operations.
     */
    void deleteByPharmacyIdAndPredictionDateBefore(Long pharmacyId, LocalDate date);
}