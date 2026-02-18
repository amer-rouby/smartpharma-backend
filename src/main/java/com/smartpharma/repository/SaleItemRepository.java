package com.smartpharma.repository;

import com.smartpharma.entity.SaleItem;
import com.smartpharma.entity.SaleTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("SELECT si FROM SaleItem si WHERE si.transaction = :transaction")
    List<SaleItem> findByTransaction(@Param("transaction") SaleTransaction transaction);

    List<SaleItem> findByProductId(Long productId);

    @Query("SELECT si FROM SaleItem si JOIN si.transaction t WHERE t.pharmacy.id = :pharmacyId")
    List<SaleItem> findByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    @Query("SELECT si FROM SaleItem si JOIN si.transaction t WHERE t.pharmacy.id = :pharmacyId AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<SaleItem> findByPharmacyIdAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT SUM(si.quantity) FROM SaleItem si WHERE si.product.id = :productId")
    Long sumQuantityByProductId(@Param("productId") Long productId);

    @Query("SELECT SUM(si.totalPrice) FROM SaleItem si WHERE si.product.id = :productId")
    java.math.BigDecimal sumTotalPriceByProductId(@Param("productId") Long productId);
}