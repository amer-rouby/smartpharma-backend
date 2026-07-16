package com.smartpharma.repository;

import com.smartpharma.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.pharmacy.id = :pharmacyId AND c.deletedAt IS NULL ORDER BY c.nameAr, c.name")
    List<Category> findByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.pharmacy.id = :pharmacyId AND c.deletedAt IS NULL")
    Optional<Category> findByIdAndPharmacyId(@Param("id") Long id, @Param("pharmacyId") Long pharmacyId);

    @Query("""
            SELECT c FROM Category c
            WHERE c.pharmacy.id = :pharmacyId
            AND (
                LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.nameAr) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.nameEn) LIKE LOWER(CONCAT('%', :query, '%'))
            )
            AND c.deletedAt IS NULL
            """)
    List<Category> searchByPharmacyIdAndName(@Param("pharmacyId") Long pharmacyId, @Param("query") String query);

    @Query("""
            SELECT COUNT(c) > 0 FROM Category c
            WHERE c.pharmacy.id = :pharmacyId
            AND c.deletedAt IS NULL
            AND (
                LOWER(c.name) = LOWER(:name)
                OR LOWER(c.nameAr) = LOWER(:name)
                OR LOWER(c.nameEn) = LOWER(:name)
            )
            """)
    boolean existsByPharmacyIdAndNameIgnoreCase(@Param("pharmacyId") Long pharmacyId, @Param("name") String name);

    @Query("SELECT COUNT(c) FROM Category c WHERE c.pharmacy.id = :pharmacyId AND c.deletedAt IS NULL")
    Long countByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("SELECT c FROM Category c WHERE c.pharmacy.id = :pharmacyId AND c.isActive = true AND c.deletedAt IS NULL")
    List<Category> findActiveByPharmacyId(@Param("pharmacyId") Long pharmacyId);
}
