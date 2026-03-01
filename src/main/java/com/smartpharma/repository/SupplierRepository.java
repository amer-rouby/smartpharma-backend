package com.smartpharma.repository;

import com.smartpharma.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    @Query("""
        SELECT s FROM Supplier s 
        WHERE s.pharmacy.id = :pharmacyId 
        AND s.deletedAt IS NULL 
        ORDER BY s.name ASC
    """)
    List<Supplier> findByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT s FROM Supplier s 
        WHERE s.pharmacy.id = :pharmacyId 
        AND s.deletedAt IS NULL
    """)
    Page<Supplier> findByPharmacyId(@Param("pharmacyId") Long pharmacyId, Pageable pageable);

    Optional<Supplier> findByIdAndPharmacyIdAndDeletedAtIsNull(Long id, Long pharmacyId);

    @Query("""
        SELECT COUNT(s) FROM Supplier s 
        WHERE s.pharmacy.id = :pharmacyId 
        AND s.deletedAt IS NULL
    """)
    Long countByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT s FROM Supplier s 
        WHERE s.pharmacy.id = :pharmacyId 
        AND s.deletedAt IS NULL
        AND (s.name LIKE %:query% OR s.contactPerson LIKE %:query% OR s.phone LIKE %:query%)
    """)
    List<Supplier> searchByPharmacyId(@Param("pharmacyId") Long pharmacyId, @Param("query") String query);

    boolean existsByPharmacyIdAndNameAndDeletedAtIsNull(Long pharmacyId, String name);
}