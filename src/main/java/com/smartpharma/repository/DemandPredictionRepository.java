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

@Repository
public interface DemandPredictionRepository extends JpaRepository<DemandPrediction, Long> {

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
            @Param("endDate") LocalDate endDate
    );

    Page<DemandPrediction> findByPharmacyId(Long pharmacyId, Pageable pageable);

    Optional<DemandPrediction> findByProductIdAndPharmacyIdAndPredictionDate(
            Long productId, Long pharmacyId, LocalDate predictionDate
    );

    @Query("SELECT COUNT(dp) FROM DemandPrediction dp WHERE dp.pharmacy.id = :pharmacyId")
    Long countByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT AVG(dp.accuracyPercentage) FROM DemandPrediction dp
        WHERE dp.pharmacy.id = :pharmacyId
        AND dp.actualQuantity IS NOT NULL
        AND dp.accuracyPercentage IS NOT NULL
    """)
    BigDecimal calculateAverageAccuracyByPharmacy(@Param("pharmacyId") Long pharmacyId);

    @Query("SELECT MAX(dp.predictionDate) FROM DemandPrediction dp WHERE dp.pharmacy.id = :pharmacyId")
    Optional<LocalDate> findLatestPredictionDateByPharmacy(@Param("pharmacyId") Long pharmacyId);

    void deleteByPharmacyIdAndPredictionDateBefore(Long pharmacyId, LocalDate date);
}