package com.smartpharma.controller;

import com.smartpharma.dto.response.ExpenseResponse;
import com.smartpharma.dto.response.FinancialReportResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports/export")
@RequiredArgsConstructor
public class ReportExportController {

    private final ReportExportService exportService;
    private final ExpenseService expenseService;
    private final ReportService reportService;

    @GetMapping("/expenses/excel")
    public ResponseEntity<byte[]> exportExpensesExcel(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) throws Exception {
        Page<ExpenseResponse> expensesPage = expenseService.getExpenses(
                pharmacyId, PageRequest.of(page, size));

        List<Map<String, Object>> expenses = expensesPage.getContent().stream()
                .map(exp -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("category", exp.getCategoryArabic());
                    map.put("title", exp.getTitle());
                    map.put("amount", exp.getAmount());
                    map.put("expenseDate", exp.getExpenseDate().toString());
                    map.put("paymentMethod", exp.getPaymentMethod());
                    map.put("referenceNumber", exp.getReferenceNumber());
                    return map;
                })
                .collect(Collectors.toList());

        byte[] excelData = exportService.exportExpensesToExcel(expenses);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expenses_" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelData);
    }

    // ✅ Export Expenses to PDF
    @GetMapping("/expenses/pdf")
    public ResponseEntity<byte[]> exportExpensesPdf(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) throws Exception {
        Page<ExpenseResponse> expensesPage = expenseService.getExpenses(
                pharmacyId, PageRequest.of(page, size));

        List<Map<String, Object>> expenses = expensesPage.getContent().stream()
                .map(exp -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("category", exp.getCategoryArabic());
                    map.put("title", exp.getTitle());
                    map.put("amount", exp.getAmount());
                    map.put("expenseDate", exp.getExpenseDate().toString());
                    map.put("paymentMethod", exp.getPaymentMethod());
                    map.put("referenceNumber", exp.getReferenceNumber());
                    return map;
                })
                .collect(Collectors.toList());

        byte[] pdfData = exportService.exportExpensesToPdf(expenses);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expenses_" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    // ✅ Export Financial Report to Excel
    @GetMapping("/financial/excel")
    public ResponseEntity<byte[]> exportFinancialExcel(
            @RequestParam Long pharmacyId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) throws Exception {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        FinancialReportResponse report = reportService.getFinancialReport(
                com.smartpharma.dto.request.ReportRequest.builder()
                        .pharmacyId(pharmacyId)
                        .startDate(startDate)
                        .endDate(endDate)
                        .build());

        List<Map<String, Object>> monthlyData = report.getMonthlyData().stream()
                .map(d -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("month", d.getMonth());
                    map.put("revenue", d.getRevenue());
                    map.put("expenses", d.getExpenses());
                    map.put("profit", d.getProfit());
                    return map;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> expensesByCategory = report.getExpensesByCategory().stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("category", c.getCategory());
                    map.put("amount", c.getAmount());
                    return map;
                })
                .collect(Collectors.toList());

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
}