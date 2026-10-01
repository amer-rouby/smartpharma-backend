package com.smartpharma.sales.repository;

import com.smartpharma.sales.entity.SaleReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

    @Query("""
        SELECT DISTINCT r FROM SaleReturn r
        LEFT JOIN FETCH r.items i
        LEFT JOIN FETCH i.saleItem si
        LEFT JOIN FETCH si.product
        WHERE r.saleTransaction.id = :saleId AND r.pharmacy.id = :pharmacyId
        ORDER BY r.returnNumber
    """)
    List<SaleReturn> findBySale(@Param("saleId") Long saleId, @Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT DISTINCT r FROM SaleReturn r
        LEFT JOIN FETCH r.items i
        LEFT JOIN FETCH i.saleItem si
        LEFT JOIN FETCH si.product
        WHERE r.id = :id AND r.pharmacy.id = :pharmacyId
    """)
    Optional<SaleReturn> findByIdAndPharmacyId(@Param("id") Long id, @Param("pharmacyId") Long pharmacyId);

    @Query("SELECT COUNT(r) > 0 FROM SaleReturn r WHERE r.saleTransaction.id = :saleId")
    boolean existsBySaleId(@Param("saleId") Long saleId);

    // [saleItemId, returned quantity] for every item of the sale returned so far.
    @Query("""
        SELECT i.saleItem.id, SUM(i.quantity) FROM SaleReturnItem i
        WHERE i.saleReturn.saleTransaction.id = :saleId
        GROUP BY i.saleItem.id
    """)
    List<Object[]> sumReturnedQuantities(@Param("saleId") Long saleId);

    // saleItemId -> quantity returned so far, for one sale.
    default Map<Long, Integer> returnedQuantitiesBySaleItem(Long saleId) {
        Map<Long, Integer> returned = new HashMap<>();
        for (Object[] row : sumReturnedQuantities(saleId)) {
            returned.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return returned;
    }
}
