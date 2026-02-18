package com.smartpharma.repository;

import com.smartpharma.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.pharmacy.id = :pharmacyId AND p.deletedAt IS NULL")
    List<Product> findByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("SELECT p FROM Product p WHERE p.pharmacy.id = :pharmacyId AND p.deletedAt IS NULL")
    Page<Product> findByPharmacyId(@Param("pharmacyId") Long pharmacyId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.pharmacy.id = :pharmacyId AND p.deletedAt IS NULL")
    Optional<Product> findByIdAndPharmacyId(@Param("id") Long id,
                                            @Param("pharmacyId") Long pharmacyId);

    @Query("SELECT p FROM Product p WHERE p.pharmacy.id = :pharmacyId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) AND p.deletedAt IS NULL")
    List<Product> findByPharmacyIdAndNameContainingIgnoreCase(@Param("pharmacyId") Long pharmacyId,
                                                              @Param("query") String query);

    @Query("SELECT p FROM Product p WHERE p.pharmacy.id = :pharmacyId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) AND p.deletedAt IS NULL")
    Page<Product> findByPharmacyIdAndNameContainingIgnoreCase(@Param("pharmacyId") Long pharmacyId,
                                                              @Param("query") String query,
                                                              Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.pharmacy.id = :pharmacyId AND p.barcode = :barcode AND p.deletedAt IS NULL")
    Optional<Product> findByPharmacyIdAndBarcode(@Param("pharmacyId") Long pharmacyId,
                                                 @Param("barcode") String barcode);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.pharmacy.id = :pharmacyId AND p.barcode = :barcode AND p.deletedAt IS NULL")
    boolean existsByPharmacyIdAndBarcode(@Param("pharmacyId") Long pharmacyId,
                                         @Param("barcode") String barcode);

    @Query("""
        SELECT p FROM Product p
        WHERE p.pharmacy.id = :pharmacyId
        AND p.deletedAt IS NULL
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :query, '%')))
    """)
    List<Product> searchProducts(@Param("pharmacyId") Long pharmacyId,
                                 @Param("query") String query);

    @Query("""
        SELECT p FROM Product p
        WHERE p.pharmacy.id = :pharmacyId
        AND p.deletedAt IS NULL
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :query, '%')))
    """)
    Page<Product> searchProducts(@Param("pharmacyId") Long pharmacyId,
                                 @Param("query") String query,
                                 Pageable pageable);

    @Query("""
        SELECT p
        FROM Product p
        LEFT JOIN p.stockBatches b
        WHERE p.pharmacy.id = :pharmacyId
        AND p.deletedAt IS NULL
        GROUP BY p
        HAVING COALESCE(SUM(
            CASE
                WHEN b.status = com.smartpharma.entity.StockBatch$BatchStatus.ACTIVE
                THEN b.quantityCurrent
                ELSE 0
            END
        ), 0) <= p.minStockLevel
    """)
    List<Product> findLowStockProducts(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT p
        FROM Product p
        LEFT JOIN p.stockBatches b
        WHERE p.pharmacy.id = :pharmacyId
        AND p.deletedAt IS NULL
        GROUP BY p
        HAVING COALESCE(SUM(
            CASE
                WHEN b.status = com.smartpharma.entity.StockBatch$BatchStatus.ACTIVE
                THEN b.quantityCurrent
                ELSE 0
            END
        ), 0) <= p.minStockLevel
    """)
    Page<Product> findLowStockProducts(@Param("pharmacyId") Long pharmacyId,
                                       Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.pharmacy.id = :pharmacyId AND p.deletedAt IS NULL")
    List<Product> findActiveProductsByPharmacy(@Param("pharmacyId") Long pharmacyId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.pharmacy.id = :pharmacyId AND p.deletedAt IS NULL")
    Long countByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT COALESCE(SUM(
            (SELECT COALESCE(SUM(
                CASE
                    WHEN b.status = com.smartpharma.entity.StockBatch$BatchStatus.ACTIVE
                    THEN b.quantityCurrent
                    ELSE 0
                END
            ),0)
            FROM StockBatch b
            WHERE b.product = p)
        * p.sellPrice), 0)
        FROM Product p
        WHERE p.pharmacy.id = :pharmacyId
        AND p.deletedAt IS NULL
    """)
    BigDecimal sumTotalInventoryValue(@Param("pharmacyId") Long pharmacyId);

    @Query("SELECT p FROM Product p WHERE p.pharmacy.id = :pharmacyId AND p.category = :category AND p.deletedAt IS NULL")
    List<Product> findByPharmacyIdAndCategory(@Param("pharmacyId") Long pharmacyId,
                                              @Param("category") String category);

    @Query("SELECT p FROM Product p WHERE p.pharmacy.id = :pharmacyId AND p.category = :category AND p.deletedAt IS NULL")
    Page<Product> findByPharmacyIdAndCategory(@Param("pharmacyId") Long pharmacyId,
                                              @Param("category") String category,
                                              Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.pharmacy.id = :pharmacyId")
    void softDelete(@Param("id") Long id,
                    @Param("pharmacyId") Long pharmacyId);
}
