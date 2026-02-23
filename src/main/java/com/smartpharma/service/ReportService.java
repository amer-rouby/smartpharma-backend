package com.smartpharma.service;

import com.smartpharma.dto.request.ReportRequest;
import com.smartpharma.dto.response.*;

public interface ReportService {
    SalesReportResponse getSalesReport(ReportRequest request);
    StockReportResponse getStockReport(ReportRequest request);
    FinancialReportResponse getFinancialReport(ReportRequest request);
    ExpiryReportResponse getExpiryReport(ReportRequest request);
}