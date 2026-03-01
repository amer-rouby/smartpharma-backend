package com.smartpharma.repository;

import com.smartpharma.entity.SaleItem;
import com.smartpharma.entity.SaleTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("SELECT si FROM SaleItem si WHERE si.transaction = :transaction")
    List<SaleItem> findByTransaction(@Param("transaction") SaleTransaction transaction);

    List<SaleItem> findByProductId(Long productId);

    @Query("SELECT si FROM SaleItem si JOIN si.transaction t WHERE t.pharmacy.id = :pharmacyId")
    List<SaleItem> findByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    // ✅ ORIGINAL: يقبل LocalDateTime (للكود القديم)
    @Query("""
        SELECT si FROM SaleItem si 
        JOIN si.transaction t 
        WHERE t.pharmacy.id = :pharmacyId 
        AND t.transactionDate BETWEEN :startDate AND :endDate
    """)
    List<SaleItem> findByPharmacyIdAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ✅ NEW: يقبل LocalDate (للكود الجديد) - باستخدام CAST
    @Query("""
        SELECT si FROM SaleItem si 
        JOIN si.transaction t 
        WHERE t.pharmacy.id = :pharmacyId 
        AND CAST(t.transactionDate AS date) BETWEEN :startDate AND :endDate
    """)
    List<SaleItem> findByPharmacyIdAndDateRangeLocalDate(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ✅ ORIGINAL: يقبل LocalDateTime (للكود القديم)
    @Query("""
        SELECT si FROM SaleItem si 
        JOIN si.transaction t 
        WHERE si.product.id = :productId 
        AND t.pharmacy.id = :pharmacyId 
        AND t.transactionDate BETWEEN :startDate AND :endDate
    """)
    List<SaleItem> findByProductIdAndPharmacyIdAndDateBetween(
            @Param("productId") Long productId,
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ✅ NEW: يقبل LocalDate (للكود الجديد) - باستخدام CAST - الحل القاطع!
    @Query("""
        SELECT si FROM SaleItem si 
        JOIN si.transaction t 
        WHERE si.product.id = :productId 
        AND t.pharmacy.id = :pharmacyId 
        AND CAST(t.transactionDate AS date) BETWEEN :startDate AND :endDate
    """)
    List<SaleItem> findByProductIdAndPharmacyIdAndDateBetweenLocalDate(
            @Param("productId") Long productId,
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(si.quantity) FROM SaleItem si WHERE si.product.id = :productId")
    Long sumQuantityByProductId(@Param("productId") Long productId);

    @Query("SELECT SUM(si.totalPrice) FROM SaleItem si WHERE si.product.id = :productId")
    BigDecimal sumTotalPriceByProductId(@Param("productId") Long productId);
}