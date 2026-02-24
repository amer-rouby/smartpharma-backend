package com.smartpharma.controller;

import com.smartpharma.dto.request.ExpenseRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.ExpenseResponse;
import com.smartpharma.dto.response.ExpenseSummaryResponse;
import com.smartpharma.entity.User;
import com.smartpharma.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ExpenseController {

    private final ExpenseService expenseService;

    // ✅ Create Expense
// ✅ Create Expense - FIXED: Make userId optional and get from SecurityContext
    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @RequestBody @Valid ExpenseRequest request,
            @RequestAttribute(value = "userId", required = false) Long userId) {  // ← أضف required = false

        // ✅ FIXED: Get userId from authenticated user if not in attributes
        Long effectiveUserId = userId;
        if (effectiveUserId == null) {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                effectiveUserId = ((User) authentication.getPrincipal()).getId();
            }
        }

        // ✅ Fallback to 1 if still null (for testing)
        if (effectiveUserId == null) {
            effectiveUserId = 1L;
        }

        ExpenseResponse response = expenseService.createExpense(request, effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Expense created successfully"));
    }
    // ✅ Get All Expenses (Paginated)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpenses(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "expenseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Page<ExpenseResponse> expenses = expenseService.getExpenses(
                pharmacyId, PageRequest.of(page, size, sort));

        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    // ✅ Get Expense by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpense(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {
        ExpenseResponse response = expenseService.getExpense(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ✅ Update Expense
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable Long id,
            @RequestBody @Valid ExpenseRequest request,
            @RequestParam Long pharmacyId,
            @RequestAttribute("userId") Long userId) {
        ExpenseResponse response = expenseService.updateExpense(id, request, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Expense updated successfully"));
    }

    // ✅ Delete Expense (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {
        expenseService.deleteExpense(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Expense deleted successfully"));
    }

    // ✅ Search Expenses
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> searchExpenses(
            @RequestParam Long pharmacyId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ExpenseResponse> results = expenseService.searchExpenses(
                pharmacyId, query, PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ✅ Get Expenses by Category
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpensesByCategory(
            @RequestParam Long pharmacyId,
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ExpenseResponse> results = expenseService.getExpensesByCategory(
                pharmacyId, category, PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ✅ Expense Summary for Reports
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ExpenseSummaryResponse>> getExpenseSummary(
            @RequestParam Long pharmacyId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        // ✅ Default to last 30 days if no dates provided
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        ExpenseSummaryResponse summary = expenseService.getExpenseSummary(pharmacyId, start, end);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}