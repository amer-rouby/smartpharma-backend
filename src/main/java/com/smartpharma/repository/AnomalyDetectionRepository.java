package com.smartpharma.repository;

import com.smartpharma.entity.AnomalyDetection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnomalyDetectionRepository extends JpaRepository<AnomalyDetection, Long> {

    Page<AnomalyDetection> findByPharmacyIdOrderByDetectedAtDesc(Long pharmacyId, Pageable pageable);

    Page<AnomalyDetection> findByPharmacyIdAndStatusOrderByDetectedAtDesc(Long pharmacyId, AnomalyDetection.Status status, Pageable pageable);

    Page<AnomalyDetection> findByPharmacyIdAndTypeOrderByDetectedAtDesc(Long pharmacyId, AnomalyDetection.Type type, Pageable pageable);

    Page<AnomalyDetection> findByPharmacyIdAndStatusAndTypeOrderByDetectedAtDesc(
            Long pharmacyId, AnomalyDetection.Status status, AnomalyDetection.Type type, Pageable pageable);

    long countByPharmacyIdAndStatus(Long pharmacyId, AnomalyDetection.Status status);

    @Query("""
        SELECT a FROM AnomalyDetection a
        WHERE a.pharmacy.id = :pharmacyId
        AND a.type = :type
        AND a.relatedEntityType = :relatedEntityType
        AND a.relatedEntityId = :relatedEntityId
        AND a.detectedAt > :since
    """)
    List<AnomalyDetection> findRecentDuplicates(
            @Param("pharmacyId") Long pharmacyId,
            @Param("type") AnomalyDetection.Type type,
            @Param("relatedEntityType") String relatedEntityType,
            @Param("relatedEntityId") Long relatedEntityId,
            @Param("since") LocalDateTime since);

    Optional<AnomalyDetection> findByIdAndPharmacyId(Long id, Long pharmacyId);
}
