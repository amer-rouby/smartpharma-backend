package com.smartpharma.controller;

import com.smartpharma.dto.request.ExpenseRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.ExpenseResponse;
import com.smartpharma.dto.response.ExpenseSummaryResponse;
import com.smartpharma.service.ExpenseService;
import com.smartpharma.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @RequestBody @Valid ExpenseRequest request) {

        request.setPharmacyId(SecurityUtils.getCurrentPharmacyId());
        Long effectiveUserId = SecurityUtils.getCurrentUserId();

        ExpenseResponse response = expenseService.createExpense(request, effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Expense created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpenses(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "expenseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Page<ExpenseResponse> expenses = expenseService.getExpenses(
                pharmacyId, PageRequest.of(page, size, sort));

        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpense(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {
        pharmacyId = SecurityUtils.getCurrentPharmacyId();
        ExpenseResponse response = expenseService.getExpense(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable Long id,
            @RequestBody @Valid ExpenseRequest request,
            @RequestParam Long pharmacyId) {
        pharmacyId = SecurityUtils.getCurrentPharmacyId();
        Long userId = SecurityUtils.getCurrentUserId();
        ExpenseResponse response = expenseService.updateExpense(id, request, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Expense updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {
        pharmacyId = SecurityUtils.getCurrentPharmacyId();
        expenseService.deleteExpense(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Expense deleted successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> searchExpenses(
            @RequestParam Long pharmacyId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

        Page<ExpenseResponse> results = expenseService.searchExpenses(
                pharmacyId, query, PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpensesByCategory(
            @RequestParam Long pharmacyId,
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

        Page<ExpenseResponse> results = expenseService.getExpensesByCategory(
                pharmacyId, category, PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ExpenseSummaryResponse>> getExpenseSummary(
            @RequestParam Long pharmacyId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        pharmacyId = SecurityUtils.getCurrentPharmacyId();

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        ExpenseSummaryResponse summary = expenseService.getExpenseSummary(pharmacyId, start, end);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}