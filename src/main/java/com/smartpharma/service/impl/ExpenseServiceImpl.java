package com.smartpharma.service.impl;

import com.smartpharma.dto.request.ExpenseRequest;
import com.smartpharma.dto.response.ExpenseResponse;
import com.smartpharma.dto.response.ExpenseSummaryResponse;
import com.smartpharma.entity.Expense;
import com.smartpharma.entity.ExpenseCategory;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.User;
import com.smartpharma.exception.ResourceNotFoundException;
import com.smartpharma.repository.ExpenseRepository;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;

    @Override
    public ExpenseResponse createExpense(ExpenseRequest request, Long userId) {
        // ✅ Validate pharmacy exists
        Pharmacy pharmacy = pharmacyRepository.findByIdAndDeletedAtIsNull(request.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found"));

        // ✅ Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // ✅ Create expense entity
        Expense expense = Expense.builder()
                .pharmacy(pharmacy)
                .category(request.getCategory())
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate())
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .attachmentUrl(request.getAttachmentUrl())
                .createdBy(user)
                .build();

        Expense saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    @Override
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request, Long pharmacyId, Long userId) {
        Expense expense = expenseRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(id, pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        // ✅ Update fields
        expense.setCategory(request.getCategory());
        expense.setTitle(request.getTitle());
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setReferenceNumber(request.getReferenceNumber());
        expense.setAttachmentUrl(request.getAttachmentUrl());

        Expense updated = expenseRepository.save(expense);
        return mapToResponse(updated);
    }

    @Override
    public void deleteExpense(Long id, Long pharmacyId) {
        Expense expense = expenseRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(id, pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        // ✅ Soft delete
        expense.markAsDeleted();
        expenseRepository.save(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(Long id, Long pharmacyId) {
        Expense expense = expenseRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(id, pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        return mapToResponse(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpenses(Long pharmacyId, Pageable pageable) {
        return expenseRepository.findByPharmacyIdAndDeletedAtIsNull(pharmacyId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> searchExpenses(Long pharmacyId, String query, Pageable pageable) {
        return expenseRepository.findByPharmacyIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(
                pharmacyId, query, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByCategory(Long pharmacyId, String category, Pageable pageable) {
        ExpenseCategory expenseCategory = ExpenseCategory.valueOf(category.toUpperCase());
        return expenseRepository.findByPharmacyIdAndCategoryAndDeletedAtIsNull(
                pharmacyId, expenseCategory, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getExpenseSummary(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal totalExpenses = expenseRepository.getTotalExpensesByDateRange(pharmacyId, startDate, endDate);
        Long totalTransactions = expenseRepository.countByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);
        BigDecimal averageExpense = totalTransactions > 0
                ? totalExpenses.divide(BigDecimal.valueOf(totalTransactions), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        // ✅ Expenses by category
        List<Object[]> categoryData = expenseRepository.getExpensesByCategory(pharmacyId, startDate, endDate);
        Map<String, BigDecimal> expensesByCategory = categoryData.stream()
                .collect(Collectors.toMap(
                        row -> ((ExpenseCategory) row[0]).getArabicName(),
                        row -> (BigDecimal) row[1]
                ));

        // ✅ Daily expenses for chart
        List<Object[]> dailyData = expenseRepository.getDailyExpenses(pharmacyId, startDate, endDate);
        List<ExpenseSummaryResponse.DailyExpenseDTO> dailyExpenses = dailyData.stream()
                .map(row -> ExpenseSummaryResponse.DailyExpenseDTO.builder()
                        .date(row[0].toString())
                        .amount((BigDecimal) row[1])
                        .count(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        // ✅ Recent expenses
        List<Expense> recent = expenseRepository.findRecentExpenses(pharmacyId, Pageable.ofSize(5));
        List<ExpenseSummaryResponse.RecentExpenseDTO> recentExpenses = recent.stream()
                .map(e -> ExpenseSummaryResponse.RecentExpenseDTO.builder()
                        .id(e.getId())
                        .title(e.getTitle())
                        .category(e.getCategory())
                        .amount(e.getAmount())
                        .expenseDate(e.getExpenseDate())
                        .build())
                .collect(Collectors.toList());

        return ExpenseSummaryResponse.builder()
                .totalExpenses(totalExpenses)
                .totalTransactions(totalTransactions)
                .averageExpense(averageExpense)
                .expensesByCategory(expensesByCategory)
                .dailyExpenses(dailyExpenses)
                .recentExpenses(recentExpenses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenses(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate) {
        return expenseRepository.getTotalExpensesByDateRange(pharmacyId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getExpensesByCategory(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate) {
        return expenseRepository.getExpensesByCategory(pharmacyId, startDate, endDate);
    }

    // ✅ Helper: Map Entity to Response DTO
    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .pharmacyId(expense.getPharmacy().getId())
                .category(expense.getCategory())
                .categoryArabic(expense.getCategory().getArabicName())
                .title(expense.getTitle())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .paymentMethod(expense.getPaymentMethod())
                .referenceNumber(expense.getReferenceNumber())
                .attachmentUrl(expense.getAttachmentUrl())
                .createdBy(expense.getCreatedBy() != null ? expense.getCreatedBy().getFullName() : null)
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}