package com.smartpharma.service;

import com.smartpharma.entity.Product;
import com.smartpharma.entity.StockBatch;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.SaleTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(Long pharmacyId) {
        log.info("Fetching dashboard stats for pharmacyId: {}", pharmacyId);

        LocalDate today = LocalDate.now();

        BigDecimal todayRevenue =
                saleTransactionRepository.sumTotalAmountByPharmacyIdAndDate(pharmacyId, today);

        Long todayOrders =
                saleTransactionRepository.countByPharmacyIdAndDate(pharmacyId, today);

        Long totalProducts =
                productRepository.countByPharmacyId(pharmacyId);

        BigDecimal inventoryValue = calculateInventoryValue(pharmacyId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayRevenue", todayRevenue != null ? todayRevenue : BigDecimal.ZERO);
        stats.put("todayOrders", todayOrders != null ? todayOrders : 0L);
        stats.put("totalProducts", totalProducts != null ? totalProducts : 0L);
        stats.put("inventoryValue", inventoryValue);

        return stats;
    }

    private BigDecimal calculateInventoryValue(Long pharmacyId) {

        List<Product> products = productRepository.findByPharmacyId(pharmacyId);

        if (products == null || products.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return products.stream()
                .map(product -> {

                    BigDecimal totalStock = product.getStockBatches().stream()
                            .filter(batch -> batch != null)
                            .filter(batch -> batch.getStatus() == StockBatch.BatchStatus.ACTIVE)
                            .filter(batch -> batch.getQuantityCurrent() != null)
                            .map(batch -> BigDecimal.valueOf(batch.getQuantityCurrent())) // 🔥 التحويل هنا
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal sellPrice =
                            product.getSellPrice() != null ? product.getSellPrice() : BigDecimal.ZERO;

                    return totalStock.multiply(sellPrice);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
