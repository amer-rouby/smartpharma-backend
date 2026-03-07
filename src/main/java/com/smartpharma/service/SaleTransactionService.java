package com.smartpharma.service;

import com.smartpharma.dto.request.SaleRequest;
import com.smartpharma.dto.response.SaleTransactionDTO;
import com.smartpharma.dto.response.SalesReportResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SaleTransactionService {

    Page<SaleTransactionDTO> getAllSales(Long pharmacyId, int page, int size);
    Page<SaleTransactionDTO> getAllSales(Long pharmacyId, int page, int size,
                                         String sortBy, String sortDirection);
    SaleTransactionDTO getSaleById(Long id, Long pharmacyId);
    SaleTransactionDTO createSale(SaleRequest request, Long currentUserId);
    SaleTransactionDTO updateSale(Long id, SaleRequest request, Long pharmacyId);
    void deleteSale(Long id, Long pharmacyId);
    SalesReportResponse getSalesAnalytics(Long pharmacyId, LocalDate startDate,
                                          LocalDate endDate, String period);
    Map<String, Object> getSalesStats(Long pharmacyId);
    Map<String, Object> getTodaySales(Long pharmacyId);
    Map<String, Object> getTodaySalesSummary(Long pharmacyId);
    Page<SaleTransactionDTO> getSalesByDateRange(Long pharmacyId,
                                                 LocalDate startDate,
                                                 LocalDate endDate);
    Page<SaleTransactionDTO> searchSales(Long pharmacyId, String query);
    List<SaleTransactionDTO> getRecentSales(Long pharmacyId, int limit);
    Map<String, Object> getSalesByCategory(Long pharmacyId,
                                           LocalDate startDate,
                                           LocalDate endDate);
    List<Map<String, Object>> getTopProducts(Long pharmacyId, int limit);
}