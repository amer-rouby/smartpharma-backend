package com.smartpharma.platform.service;


import com.smartpharma.platform.dto.response.ExpiryReportResponse;
import com.smartpharma.platform.dto.response.FinancialReportResponse;
import com.smartpharma.platform.dto.response.StockReportResponse;
import com.smartpharma.sales.dto.response.SalesReportResponse;
import com.smartpharma.platform.dto.request.ReportRequest;

public interface ReportService {
    SalesReportResponse getSalesReport(ReportRequest request);
    StockReportResponse getStockReport(ReportRequest request);
    FinancialReportResponse getFinancialReport(ReportRequest request);
    ExpiryReportResponse getExpiryReport(ReportRequest request);
}