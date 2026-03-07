package com.smartpharma.repository;

import com.smartpharma.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);

    @Query("""
        SELECT poi FROM PurchaseOrderItem poi 
        JOIN poi.purchaseOrder po 
        WHERE po.pharmacy.id = :pharmacyId 
        AND po.deletedAt IS NULL
    """)
    List<PurchaseOrderItem> findByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    void deleteByPurchaseOrderId(Long purchaseOrderId);

    @Query("""
        SELECT SUM(poi.quantity) FROM PurchaseOrderItem poi 
        JOIN poi.purchaseOrder po 
        WHERE poi.product.id = :productId 
        AND po.pharmacy.id = :pharmacyId 
        AND po.status = 'RECEIVED'
        AND po.deletedAt IS NULL
    """)
    Long sumQuantityByProductIdAndPharmacyId(@Param("productId") Long productId,
                                             @Param("pharmacyId") Long pharmacyId);
}