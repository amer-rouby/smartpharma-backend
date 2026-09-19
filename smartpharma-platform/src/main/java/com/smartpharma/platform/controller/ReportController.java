package com.smartpharma.platform.controller;


import com.smartpharma.common.dto.ApiResponse;
import com.smartpharma.platform.dto.response.ExpiryReportResponse;
import com.smartpharma.platform.dto.response.FinancialReportResponse;
import com.smartpharma.platform.dto.response.StockReportResponse;
import com.smartpharma.sales.dto.response.SalesReportResponse;
import com.smartpharma.platform.dto.request.ReportRequest;
import com.smartpharma.platform.service.ReportService;
import com.smartpharma.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PHARMACIST')")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/sales")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesReport(
            @RequestBody ReportRequest request) {
        request.setPharmacyId(SecurityUtils.getCurrentPharmacyId());
        SalesReportResponse response = reportService.getSalesReport(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/stock")
    public ResponseEntity<ApiResponse<StockReportResponse>> getStockReport(
            @RequestBody ReportRequest request) {
        request.setPharmacyId(SecurityUtils.getCurrentPharmacyId());
        StockReportResponse response = reportService.getStockReport(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/financial")
    public ResponseEntity<ApiResponse<FinancialReportResponse>> getFinancialReport(
            @RequestBody ReportRequest request) {
        request.setPharmacyId(SecurityUtils.getCurrentPharmacyId());
        FinancialReportResponse response = reportService.getFinancialReport(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/expiry")
    public ResponseEntity<ApiResponse<ExpiryReportResponse>> getExpiryReport(
            @RequestBody ReportRequest request) {
        request.setPharmacyId(SecurityUtils.getCurrentPharmacyId());
        ExpiryReportResponse response = reportService.getExpiryReport(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
