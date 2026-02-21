package com.smartpharma.service;

import com.smartpharma.dto.request.SaleRequest;
import com.smartpharma.dto.response.SaleTransactionDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SaleTransactionService {

    Page<SaleTransactionDTO> getAllSales(Long pharmacyId, int page, int size);

    SaleTransactionDTO getSaleById(Long id, Long pharmacyId);

    SaleTransactionDTO createSale(SaleRequest request, Long currentUserId);

    SaleTransactionDTO updateSale(Long id, SaleRequest request, Long pharmacyId);

    void deleteSale(Long id, Long pharmacyId);

    Map<String, Object> getTodaySales(Long pharmacyId);

    Map<String, Object> getTodaySalesSummary(Long pharmacyId);

    Map<String, Object> getSalesStats(Long pharmacyId);

    Page<SaleTransactionDTO> getSalesByDateRange(Long pharmacyId, LocalDate startDate, LocalDate endDate);

    Page<SaleTransactionDTO> searchSales(Long pharmacyId, String query);

    List<SaleTransactionDTO> getRecentSales(Long pharmacyId, int limit);
}