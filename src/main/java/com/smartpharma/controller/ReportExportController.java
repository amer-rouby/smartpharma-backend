package com.smartpharma.controller;

import com.smartpharma.dto.request.ReportRequest;
import com.smartpharma.dto.response.*;
import com.smartpharma.service.ExpenseService;
import com.smartpharma.service.ReportExportService;
import com.smartpharma.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports/export")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PHARMACIST')")
@CrossOrigin(origins = "http://localhost:4200")
public class ReportExportController {

    private final ReportExportService exportService;
    private final ExpenseService expenseService;
    private final ReportService reportService;

    // ✅ Expenses Export - Excel
    @GetMapping("/expenses/excel")
    public ResponseEntity<byte[]> exportExpensesExcel(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        Page<ExpenseResponse> expensesPage = expenseService.getExpenses(pharmacyId, PageRequest.of(page, size));

        List<Map<String, Object>> expenses = expensesPage.getContent().stream().map(exp -> {
            Map<String, Object> map = new HashMap<>();
            map.put("category", exp.getCategoryArabic());
            map.put("title", exp.getTitle());
            map.put("amount", exp.getAmount());
            map.put("expenseDate", exp.getExpenseDate().toString());
            map.put("paymentMethod", exp.getPaymentMethod());
            map.put("referenceNumber", exp.getReferenceNumber());
            return map;
        }).collect(Collectors.toList());

        byte[] excelData = exportService.exportExpensesToExcel(expenses);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expenses_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelData);
    }

    // ✅ Expenses Export - PDF
    @GetMapping("/expenses/pdf")
    public ResponseEntity<byte[]> exportExpensesPdf(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        Page<ExpenseResponse> expensesPage = expenseService.getExpenses(pharmacyId, PageRequest.of(page, size));

        List<Map<String, Object>> expenses = expensesPage.getContent().stream().map(exp -> {
            Map<String, Object> map = new HashMap<>();
            map.put("category", exp.getCategoryArabic());
            map.put("title", exp.getTitle());
            map.put("amount", exp.getAmount());
            map.put("expenseDate", exp.getExpenseDate().toString());
            map.put("paymentMethod", exp.getPaymentMethod());
            map.put("referenceNumber", exp.getReferenceNumber());
            return map;
        }).collect(Collectors.toList());

        byte[] pdfData = exportService.exportExpensesToPdf(expenses);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expenses_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    // ✅ Financial Report Export - Excel
    @GetMapping("/financial/excel")
    public ResponseEntity<byte[]> exportFinancialExcel(
            @RequestParam Long pharmacyId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        FinancialReportResponse report = reportService.getFinancialReport(
                ReportRequest.builder().pharmacyId(pharmacyId).startDate(startDate).endDate(endDate).build());

        List<Map<String, Object>> monthlyData = report.getMonthlyData().stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("month", d.getMonth());
            map.put("revenue", d.getRevenue());
            map.put("expenses", d.getExpenses());
            map.put("profit", d.getProfit());
            return map;
        }).collect(Collectors.toList());

        List<Map<String, Object>> expensesByCategory = report.getExpensesByCategory().stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("category", c.getCategory());
            map.put("amount", c.getAmount());
            return map;
        }).collect(Collectors.toList());

        byte[] excelData = exportService.exportFinancialReportToExcel(
                report.getTotalRevenue().doubleValue(),
                report.getTotalExpenses().doubleValue(),
                report.getNetProfit().doubleValue(),
                report.getProfitMargin().doubleValue(),
                monthlyData,
                expensesByCategory
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financial_report_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelData);
    }

    // ✅ Sales Report Export - Excel (جديد)
    @GetMapping("/sales/excel")
    public ResponseEntity<byte[]> exportSalesExcel(
            @RequestParam Long pharmacyId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        SalesReportResponse report = reportService.getSalesReport(
                ReportRequest.builder().pharmacyId(pharmacyId).startDate(startDate).endDate(endDate).build());

        List<Map<String, Object>> dailySales = report.getDailySales().stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", d.getDate());
            map.put("revenue", d.getRevenue());
            map.put("orders", d.getOrders());
            return map;
        }).collect(Collectors.toList());

        List<Map<String, Object>> topProducts = report.getTopProducts().stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("productName", p.getProductName());
            map.put("quantitySold", p.getQuantitySold());
            map.put("totalRevenue", p.getTotalRevenue());
            return map;
        }).collect(Collectors.toList());

        byte[] excelData = exportService.exportSalesReportToExcel(
                report.getTotalRevenue().doubleValue(),
                report.getTotalOrders(),
                report.getAverageOrder().doubleValue(),
                dailySales,
                topProducts,
                report.getRevenueByPaymentMethod()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_report_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelData);
    }

    // ✅ Sales Report Export - PDF (جديد)
    @GetMapping("/sales/pdf")
    public ResponseEntity<byte[]> exportSalesPdf(
            @RequestParam Long pharmacyId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        SalesReportResponse report = reportService.getSalesReport(
                ReportRequest.builder().pharmacyId(pharmacyId).startDate(startDate).endDate(endDate).build());

        List<Map<String, Object>> dailySales = report.getDailySales().stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", d.getDate());
            map.put("revenue", d.getRevenue());
            map.put("orders", d.getOrders());
            return map;
        }).collect(Collectors.toList());

        List<Map<String, Object>> topProducts = report.getTopProducts().stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("productName", p.getProductName());
            map.put("quantitySold", p.getQuantitySold());
            map.put("totalRevenue", p.getTotalRevenue());
            return map;
        }).collect(Collectors.toList());

        byte[] pdfData = exportService.exportSalesReportToPdf(
                report.getTotalRevenue().doubleValue(),
                report.getTotalOrders(),
                report.getAverageOrder().doubleValue(),
                dailySales,
                topProducts,
                report.getRevenueByPaymentMethod()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_report_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    // ✅ Expiry Report Export - Excel (جديد)
    @GetMapping("/expiry/excel")
    public ResponseEntity<byte[]> exportExpiryExcel(@RequestParam Long pharmacyId) {
        ExpiryReportResponse report = reportService.getExpiryReport(
                ReportRequest.builder().pharmacyId(pharmacyId).build());

        List<Map<String, Object>> expiringProducts = report.getExpiringProducts().stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("productName", p.getProductName());
            map.put("batchNumber", p.getBatchNumber());
            map.put("expiryDate", p.getExpiryDate());
            map.put("daysUntilExpiry", p.getDaysUntilExpiry());
            map.put("currentStock", p.getCurrentStock());
            map.put("status", p.getStatus());
            return map;
        }).collect(Collectors.toList());

        byte[] excelData = exportService.exportExpiryReportToExcel(
                report.getTotalExpiring(),
                report.getUrgentExpiring(),
                report.getWarningExpiring(),
                report.getOkExpiring(),
                expiringProducts
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expiry_report_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelData);
    }

    // ✅ Expiry Report Export - PDF (جديد)
    @GetMapping("/expiry/pdf")
    public ResponseEntity<byte[]> exportExpiryPdf(@RequestParam Long pharmacyId) {
        ExpiryReportResponse report = reportService.getExpiryReport(
                ReportRequest.builder().pharmacyId(pharmacyId).build());

        List<Map<String, Object>> expiringProducts = report.getExpiringProducts().stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("productName", p.getProductName());
            map.put("batchNumber", p.getBatchNumber());
            map.put("expiryDate", p.getExpiryDate());
            map.put("daysUntilExpiry", p.getDaysUntilExpiry());
            map.put("currentStock", p.getCurrentStock());
            map.put("status", p.getStatus());
            return map;
        }).collect(Collectors.toList());

        byte[] pdfData = exportService.exportExpiryReportToPdf(
                report.getTotalExpiring(),
                report.getUrgentExpiring(),
                report.getWarningExpiring(),
                report.getOkExpiring(),
                expiringProducts
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expiry_report_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}