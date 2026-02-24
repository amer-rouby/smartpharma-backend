package com.smartpharma.service.impl;

import com.smartpharma.dto.request.ReportRequest;
import com.smartpharma.dto.response.*;
import com.smartpharma.entity.ExpenseCategory;
import com.smartpharma.repository.ExpenseRepository;
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
    private final ExpenseRepository expenseRepository;

    // ✅ Helper Methods: Convert LocalDate to LocalDateTime
    private LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : LocalDateTime.now();
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59) : LocalDateTime.now();
    }

    @Override
    public SalesReportResponse getSalesReport(ReportRequest request) {
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

        // ✅ FIXED: PaymentMethod is Enum, convert to String properly
        List<Object[]> paymentData = saleRepository.getRevenueByPaymentMethod(
                pharmacyId, startDateTime, endDateTime);

        Map<String, BigDecimal> revenueByPayment = paymentData.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        row -> row[0] != null ? row[0].toString() : "UNKNOWN",
                        row -> (BigDecimal) row[1],
                        BigDecimal::add
                ));

        List<Object[]> topProductsData = saleRepository.getTopProducts(
                pharmacyId, startDateTime, endDateTime, PageRequest.of(0, 5));

        List<SalesReportResponse.TopProductDTO> topProducts = topProductsData.stream()
                .filter(Objects::nonNull)
                .map(row -> SalesReportResponse.TopProductDTO.builder()
                        .productId(row[0] != null ? ((Number) row[0]).longValue() : 0L)
                        .productName((String) row[1])
                        .quantitySold(row[2] != null ? ((Number) row[2]).longValue() : 0L)
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
                        .orders(row[2] != null ? ((Number) row[2]).longValue() : 0L)
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

        // ✅ Stock by category
        List<Object[]> categoryData = stockBatchRepository.getStockByCategory(pharmacyId);
        List<StockReportResponse.StockByCategoryDTO> stockByCategory = categoryData.stream()
                .filter(Objects::nonNull)
                .map(row -> StockReportResponse.StockByCategoryDTO.builder()
                        .categoryName(row[0] != null ? (String) row[0] : "غير مصنف")
                        .itemCount(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                        .totalValue(row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        // ✅ Low stock products - FIXED: Use (Number).intValue() for aggregates
        List<Object[]> lowStockData = stockBatchRepository.getLowStockProducts(pharmacyId);
        List<StockReportResponse.StockItemDTO> lowStockProducts = lowStockData.stream()
                .filter(Objects::nonNull)
                .map(row -> StockReportResponse.StockItemDTO.builder()
                        .productId(row[0] != null ? ((Number) row[0]).longValue() : 0L)
                        .productName((String) row[1])
                        .batchNumber((String) row[2])
                        // ✅ FIXED: SUM returns Long/BigDecimal, not Integer
                        .currentStock(row[3] != null ? ((Number) row[3]).intValue() : 0)
                        // ✅ FIXED: minStockLevel might be Long
                        .minStock(row[4] != null ? ((Number) row[4]).intValue() : 0)
                        .expiryDate(row[5] != null ? row[5].toString() : null)
                        .build())
                .collect(Collectors.toList());

        // ✅ Expiring products - FIXED: Use (Number).intValue() for aggregates
        List<Object[]> expiringData = stockBatchRepository.getExpiringProducts(
                pharmacyId, LocalDate.now().plusDays(30));

        List<StockReportResponse.StockItemDTO> expiringProducts = expiringData.stream()
                .filter(Objects::nonNull)
                .map(row -> {
                    LocalDate expiryDate = (LocalDate) row[3];
                    long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

                    return StockReportResponse.StockItemDTO.builder()
                            .productId(row[0] != null ? ((Number) row[0]).longValue() : 0L)
                            .productName((String) row[1])
                            .batchNumber((String) row[2])
                            // ✅ FIXED: currentStock from SUM might be Long
                            .currentStock(row[4] != null ? ((Number) row[4]).intValue() : 0)
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

        // ✅ CONVERT to LocalDateTime for Repository queries
        LocalDateTime startDateTime = toStartOfDay(startDate);
        LocalDateTime endDateTime = toEndOfDay(endDate);

        // ✅ Revenue from sales
        BigDecimal totalRevenue = saleRepository.getTotalRevenue(pharmacyId, startDateTime, endDateTime);

        // ✅ Expenses from Expense module
        BigDecimal totalExpenses = expenseRepository.getTotalExpensesByDateRange(
                pharmacyId, startDateTime, endDateTime);

        // ✅ Calculate profit
        BigDecimal netProfit = totalRevenue.subtract(totalExpenses);
        BigDecimal profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                netProfit.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, BigDecimal.ROUND_HALF_UP) :
                BigDecimal.ZERO;

        // ✅ Monthly/Daily data for charts (combine sales + expenses)
        List<Object[]> dailySalesData = saleRepository.getDailySales(pharmacyId, startDateTime, endDateTime);
        List<Object[]> dailyExpensesData = expenseRepository.getDailyExpenses(pharmacyId, startDateTime, endDateTime);

        // ✅ Merge sales and expenses by date - FIXED: Rebuild DTO instead of mutating
        Map<String, FinancialReportResponse.MonthlyFinancialDTO> monthlyMap = new LinkedHashMap<>();

        // Add sales data first
        if (dailySalesData != null) {
            for (Object[] row : dailySalesData) {
                String date = row[0] != null ? row[0].toString() : "";
                BigDecimal revenue = (BigDecimal) row[1];
                BigDecimal revenueValue = revenue != null ? revenue : BigDecimal.ZERO;

                monthlyMap.put(date, FinancialReportResponse.MonthlyFinancialDTO.builder()
                        .month(date)
                        .revenue(revenueValue)
                        .expenses(BigDecimal.ZERO)
                        .profit(revenueValue)  // ✅ profit = revenue initially (no expenses yet)
                        .build());
            }
        }

        // Merge expenses data - FIXED: Rebuild DTO instead of using setters
        if (dailyExpensesData != null) {
            for (Object[] row : dailyExpensesData) {
                String date = row[0] != null ? row[0].toString() : "";
                BigDecimal expenses = (BigDecimal) row[1];
                BigDecimal expensesValue = expenses != null ? expenses : BigDecimal.ZERO;

                if (monthlyMap.containsKey(date)) {
                    // ✅ Rebuild the DTO with merged values (because @Builder creates immutable objects)
                    FinancialReportResponse.MonthlyFinancialDTO existing = monthlyMap.get(date);
                    BigDecimal revenue = existing.getRevenue() != null ? existing.getRevenue() : BigDecimal.ZERO;

                    monthlyMap.put(date, FinancialReportResponse.MonthlyFinancialDTO.builder()
                            .month(date)
                            .revenue(revenue)
                            .expenses(expensesValue)
                            .profit(revenue.subtract(expensesValue))  // ✅ Correct profit calculation
                            .build());
                } else {
                    // Expenses only, no sales for this date
                    monthlyMap.put(date, FinancialReportResponse.MonthlyFinancialDTO.builder()
                            .month(date)
                            .revenue(BigDecimal.ZERO)
                            .expenses(expensesValue)
                            .profit(BigDecimal.ZERO.subtract(expensesValue))  // ✅ Negative profit
                            .build());
                }
            }
        }

        List<FinancialReportResponse.MonthlyFinancialDTO> monthlyData = new ArrayList<>(monthlyMap.values());

        // ✅ Expenses by category
        List<Object[]> categoryData = expenseRepository.getExpensesByCategory(
                pharmacyId, startDateTime, endDateTime);

        List<FinancialReportResponse.CategoryExpenseDTO> expensesByCategory = categoryData.stream()
                .filter(Objects::nonNull)
                .map(row -> {
                    ExpenseCategory category = (ExpenseCategory) row[0];
                    BigDecimal amount = (BigDecimal) row[1];
                    return FinancialReportResponse.CategoryExpenseDTO.builder()
                            .category(category != null ? category.getArabicName() : "أخرى")
                            .amount(amount != null ? amount : BigDecimal.ZERO)
                            .build();
                })
                .collect(Collectors.toList());

        return FinancialReportResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalExpenses(totalExpenses != null ? totalExpenses : BigDecimal.ZERO)
                .netProfit(netProfit != null ? netProfit : BigDecimal.ZERO)
                .profitMargin(profitMargin != null ? profitMargin : BigDecimal.ZERO)
                .monthlyData(monthlyData)
                .expensesByCategory(expensesByCategory)
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
                            .productId(row[0] != null ? ((Number) row[0]).longValue() : 0L)
                            .productName((String) row[1])
                            .batchNumber((String) row[2])
                            .expiryDate(expiryDate.toString())
                            .daysUntilExpiry((int) daysUntil)
                            // ✅ FIXED: currentStock from SUM might be Long
                            .currentStock(row[4] != null ? ((Number) row[4]).intValue() : 0)
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