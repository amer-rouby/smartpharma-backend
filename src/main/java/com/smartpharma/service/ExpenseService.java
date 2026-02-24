package com.smartpharma.service;

import com.smartpharma.dto.request.ExpenseRequest;
import com.smartpharma.dto.response.ExpenseResponse;
import com.smartpharma.dto.response.ExpenseSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseService {

    // ✅ CRUD Operations
    ExpenseResponse createExpense(ExpenseRequest request, Long userId);
    ExpenseResponse updateExpense(Long id, ExpenseRequest request, Long pharmacyId, Long userId);
    void deleteExpense(Long id, Long pharmacyId);
    ExpenseResponse getExpense(Long id, Long pharmacyId);
    Page<ExpenseResponse> getExpenses(Long pharmacyId, Pageable pageable);

    // ✅ Filter & Search
    Page<ExpenseResponse> searchExpenses(Long pharmacyId, String query, Pageable pageable);
    Page<ExpenseResponse> getExpensesByCategory(Long pharmacyId, String category, Pageable pageable);

    // ✅ Reports & Analytics
    ExpenseSummaryResponse getExpenseSummary(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate);
    BigDecimal getTotalExpenses(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate);
    List<Object[]> getExpensesByCategory(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate);
}