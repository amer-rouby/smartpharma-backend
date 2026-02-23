package com.smartpharma.service.impl;

import com.smartpharma.dto.request.ReportRequest;
import com.smartpharma.dto.response.*;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.SaleTransactionRepository;
import com.smartpharma.repository.StockBatchRepository;
import com.smartpharma.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final SaleTransactionRepository saleRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;

    // ✅ Helper Methods: Convert LocalDate to LocalDateTime
    private LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : LocalDateTime.now();
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59) : LocalDateTime.now();
    }

    @Override
    public SalesReportResponse getSalesReport(ReportRequest request) {
        // ✅ Get as LocalDate from DTO
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        Long pharmacyId = request.getPharmacyId();

        // ✅ CONVERT to LocalDateTime for Repository (DB column is TIMESTAMP)
        LocalDateTime startDateTime = toStartOfDay(startDate);
        LocalDateTime endDateTime = toEndOfDay(endDate);

        // ✅ Repository expects LocalDateTime
        BigDecimal totalRevenue = saleRepository.getTotalRevenue(pharmacyId, startDateTime, endDateTime);
        Long totalOrders = saleRepository.getTotalOrders(pharmacyId, startDateTime, endDateTime);

        BigDecimal averageOrder = totalOrders != null && totalOrders > 0 ?
                totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP) :
                BigDecimal.ZERO;

        // ✅ ✅ ✅ FIXED: PaymentMethod is Enum, convert to String properly ✅ ✅ ✅
        List<Object[]> paymentData = saleRepository.getRevenueByPaymentMethod(
                pharmacyId, startDateTime, endDateTime);

        Map<String, BigDecimal> revenueByPayment = paymentData.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        // ✅ FIXED: Convert Enum to String using toString()
                        row -> row[0] != null ? row[0].toString() : "UNKNOWN",
                        row -> (BigDecimal) row[1],
                        BigDecimal::add
                ));

        List<Object[]> topProductsData = saleRepository.getTopProducts(
                pharmacyId, startDateTime, endDateTime, PageRequest.of(0, 5));

        List<SalesReportResponse.TopProductDTO> topProducts = topProductsData.stream()
                .filter(Objects::nonNull)
                .map(row -> SalesReportResponse.TopProductDTO.builder()
                        .productId(((Number) row[0]).longValue())
                        .productName((String) row[1])
                        .quantitySold(((Number) row[2]).longValue())
                        .totalRevenue((BigDecimal) row[3])
                        .build())
                .collect(Collectors.toList());

        List<Object[]> dailySalesData = saleRepository.getDailySales(
                pharmacyId, startDateTime, endDateTime);

        List<SalesReportResponse.DailySalesDTO> dailySales = dailySalesData.stream()
                .filter(Objects::nonNull)
                .map(row -> SalesReportResponse.DailySalesDTO.builder()
                        .date(row[0] != null ? row[0].toString() : "")
                        .revenue((BigDecimal) row[1])
                        .orders(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        return SalesReportResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalOrders(totalOrders != null ? totalOrders : 0L)
                .averageOrder(averageOrder)
                .totalItems(totalOrders != null ? totalOrders : 0L)
                .revenueByPaymentMethod(revenueByPayment)
                .topProducts(topProducts)
                .dailySales(dailySales)
                .build();
    }

    @Override
    public StockReportResponse getStockReport(ReportRequest request) {
        Long pharmacyId = request.getPharmacyId();

        BigDecimal totalValue = stockBatchRepository.getTotalStockValue(pharmacyId);
        Long totalItems = stockBatchRepository.getTotalStockItemsCount(pharmacyId);
        Long lowStock = stockBatchRepository.countLowStockItems(pharmacyId);
        Long expired = stockBatchRepository.countExpiredBatches(pharmacyId);
        Long expiringSoon = stockBatchRepository.countExpiringBatches(
                pharmacyId, LocalDate.now().plusDays(30));

        List<Object[]> categoryData = stockBatchRepository.getStockByCategory(pharmacyId);
        List<StockReportResponse.StockByCategoryDTO> stockByCategory = categoryData.stream()
                .filter(Objects::nonNull)
                .map(row -> StockReportResponse.StockByCategoryDTO.builder()
                        .categoryName(row[0] != null ? (String) row[0] : "غير مصنف")
                        .itemCount(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                        .totalValue(row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        List<Object[]> lowStockData = stockBatchRepository.getLowStockProducts(pharmacyId);
        List<StockReportResponse.StockItemDTO> lowStockProducts = lowStockData.stream()
                .filter(Objects::nonNull)
                .map(row -> StockReportResponse.StockItemDTO.builder()
                        .productId(((Number) row[0]).longValue())
                        .productName((String) row[1])
                        .batchNumber((String) row[2])
                        .currentStock(row[3] != null ? (Integer) row[3] : 0)
                        .minStock(row[4] != null ? (Integer) row[4] : 0)
                        .expiryDate(row[5] != null ? row[5].toString() : null)
                        .build())
                .collect(Collectors.toList());

        List<Object[]> expiringData = stockBatchRepository.getExpiringProducts(
                pharmacyId, LocalDate.now().plusDays(30));

        List<StockReportResponse.StockItemDTO> expiringProducts = expiringData.stream()
                .filter(Objects::nonNull)
                .map(row -> {
                    LocalDate expiryDate = (LocalDate) row[3];
                    long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

                    return StockReportResponse.StockItemDTO.builder()
                            .productId(((Number) row[0]).longValue())
                            .productName((String) row[1])
                            .batchNumber((String) row[2])
                            .currentStock(row[4] != null ? (Integer) row[4] : 0)
                            .expiryDate(expiryDate.toString())
                            .daysUntilExpiry((int) daysUntil)
                            .build();
                })
                .collect(Collectors.toList());

        return StockReportResponse.builder()
                .totalStockValue(totalValue != null ? totalValue : BigDecimal.ZERO)
                .totalItems(totalItems != null ? totalItems : 0L)
                .lowStockItems(lowStock != null ? lowStock : 0L)
                .expiredItems(expired != null ? expired : 0L)
                .expiringSoonItems(expiringSoon != null ? expiringSoon : 0L)
                .stockByCategory(stockByCategory)
                .lowStockProducts(lowStockProducts)
                .expiringProducts(expiringProducts)
                .build();
    }

    @Override
    public FinancialReportResponse getFinancialReport(ReportRequest request) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        Long pharmacyId = request.getPharmacyId();

        LocalDateTime startDateTime = toStartOfDay(startDate);
        LocalDateTime endDateTime = toEndOfDay(endDate);

        BigDecimal totalRevenue = saleRepository.getTotalRevenue(pharmacyId, startDateTime, endDateTime);
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal netProfit = totalRevenue != null ? totalRevenue.subtract(totalExpenses) : BigDecimal.ZERO;
        BigDecimal profitMargin = totalRevenue != null && totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                netProfit.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, BigDecimal.ROUND_HALF_UP) :
                BigDecimal.ZERO;

        return FinancialReportResponse.builder()
                .totalRevenue(totalRevenue)
                .totalExpenses(totalExpenses)
                .netProfit(netProfit)
                .profitMargin(profitMargin)
                .monthlyData(new ArrayList<>())
                .expensesByCategory(new ArrayList<>())
                .build();
    }

    @Override
    public ExpiryReportResponse getExpiryReport(ReportRequest request) {
        Long pharmacyId = request.getPharmacyId();

        Long totalExpiring = stockBatchRepository.countExpiringBatches(
                pharmacyId, LocalDate.now().plusDays(90));
        Long urgent = stockBatchRepository.countExpiringBatches(
                pharmacyId, LocalDate.now().plusDays(7));
        Long warning = stockBatchRepository.countExpiringBatches(
                pharmacyId, LocalDate.now().plusDays(30));
        Long ok = (totalExpiring != null ? totalExpiring : 0L) - (warning != null ? warning : 0L);

        List<Object[]> expiringData = stockBatchRepository.getExpiringProducts(
                pharmacyId, LocalDate.now().plusDays(90));

        List<ExpiryReportResponse.ExpiringProductDTO> expiringProducts = expiringData.stream()
                .filter(Objects::nonNull)
                .map(row -> {
                    LocalDate expiryDate = (LocalDate) row[3];
                    long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

                    String status;
                    if (daysUntil <= 7) {
                        status = "URGENT";
                    } else if (daysUntil <= 30) {
                        status = "WARNING";
                    } else {
                        status = "OK";
                    }

                    return ExpiryReportResponse.ExpiringProductDTO.builder()
                            .productId(((Number) row[0]).longValue())
                            .productName((String) row[1])
                            .batchNumber((String) row[2])
                            .expiryDate(expiryDate.toString())
                            .daysUntilExpiry((int) daysUntil)
                            .currentStock(row[4] != null ? (Integer) row[4] : 0)
                            .status(status)
                            .estimatedValue(0.0)
                            .build();
                })
                .collect(Collectors.toList());

        return ExpiryReportResponse.builder()
                .totalExpiring(totalExpiring != null ? totalExpiring : 0L)
                .urgentExpiring(urgent != null ? urgent : 0L)
                .warningExpiring(warning != null ? warning : 0L)
                .okExpiring(ok)
                .expiringProducts(expiringProducts)
                .build();
    }
}