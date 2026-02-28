package com.smartpharma.service.impl;

import com.smartpharma.dto.response.DashboardResponse;
import com.smartpharma.entity.Product;
import com.smartpharma.entity.SaleTransaction;
import com.smartpharma.entity.StockBatch;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.SaleTransactionRepository;
import com.smartpharma.repository.StockBatchRepository;
import com.smartpharma.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardStats(Long pharmacyId) {
        log.info("Fetching dashboard stats for pharmacyId: {}", pharmacyId);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        BigDecimal todayRevenue = saleTransactionRepository
                .sumTotalAmountByPharmacyIdAndDateRange(pharmacyId, startOfDay, endOfDay);

        Long todayOrders = saleTransactionRepository
                .countByPharmacyIdAndDateRange(pharmacyId, startOfDay, endOfDay);

        BigDecimal todayAverageOrder = (todayOrders != null && todayOrders > 0 && todayRevenue != null)
                ? todayRevenue.divide(BigDecimal.valueOf(todayOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Long totalProducts = productRepository.countByPharmacyId(pharmacyId);
        Long lowStockProducts = productRepository.countLowStockProducts(pharmacyId);
        Long outOfStockProducts = productRepository.countOutOfStockProducts(pharmacyId);
        BigDecimal inventoryValue = calculateInventoryValue(pharmacyId);

        LocalDate expiryThreshold = LocalDate.now().plusDays(30);
        Long expiringBatches = stockBatchRepository.countExpiringBatches(pharmacyId, expiryThreshold);
        Long expiredBatches = stockBatchRepository.countExpiredBatches(pharmacyId);
        
        List<DashboardResponse.TopProductDTO> topProducts = getTopProducts(pharmacyId, 5);

        List<DashboardResponse.RecentSaleDTO> recentSales = getRecentSales(pharmacyId, 5);

        return DashboardResponse.builder()
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .todayOrders(todayOrders != null ? todayOrders : 0L)
                .todayAverageOrder(todayAverageOrder)
                .totalProducts(totalProducts != null ? totalProducts : 0L)
                .lowStockProducts(lowStockProducts != null ? lowStockProducts : 0L)
                .outOfStockProducts(outOfStockProducts != null ? outOfStockProducts : 0L)
                .inventoryValue(inventoryValue != null ? inventoryValue : BigDecimal.ZERO)
                .expiringBatches(expiringBatches != null ? expiringBatches : 0L)
                .expiredBatches(expiredBatches != null ? expiredBatches : 0L)
                .topProducts(topProducts)
                .recentSales(recentSales)
                .build();
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
                            .map(batch -> BigDecimal.valueOf(batch.getQuantityCurrent()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal sellPrice = product.getSellPrice() != null
                            ? product.getSellPrice() : BigDecimal.ZERO;

                    return totalStock.multiply(sellPrice);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<DashboardResponse.TopProductDTO> getTopProducts(Long pharmacyId, int limit) {
        List<Object[]> results = saleTransactionRepository.findTopSellingProducts(
                pharmacyId, PageRequest.of(0, limit));

        return results.stream()
                .map(obj -> DashboardResponse.TopProductDTO.builder()
                        .productId(obj[0] != null ? ((Number) obj[0]).longValue() : 0L)
                        .productName(obj[1] != null ? (String) obj[1] : "")
                        .quantitySold(obj[2] != null ? ((Number) obj[2]).longValue() : 0L)
                        .totalRevenue(obj[3] != null ? (BigDecimal) obj[3] : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardResponse.RecentSaleDTO> getRecentSales(Long pharmacyId, int limit) {
        List<SaleTransaction> sales = saleTransactionRepository.findRecentSalesByPharmacyId(
                pharmacyId, PageRequest.of(0, limit));

        return sales.stream()
                .map(sale -> DashboardResponse.RecentSaleDTO.builder()
                        .saleId(sale.getId())
                        .invoiceNumber(sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "")
                        .totalAmount(sale.getTotalAmount() != null ? sale.getTotalAmount() : BigDecimal.ZERO)
                        .transactionDate(sale.getTransactionDate() != null ? sale.getTransactionDate().toString() : "")
                        .paymentMethod(sale.getPaymentMethod() != null ? sale.getPaymentMethod().name() : "CASH")
                        .build())
                .collect(Collectors.toList());
    }
}