package com.smartpharma.dto.response;

import com.smartpharma.entity.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSummaryResponse {

    private BigDecimal totalExpenses;
    private Long totalTransactions;
    private BigDecimal averageExpense;
    private Map<String, BigDecimal> expensesByCategory;
    private List<DailyExpenseDTO> dailyExpenses;
    private List<RecentExpenseDTO> recentExpenses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyExpenseDTO {
        private String date;
        private BigDecimal amount;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentExpenseDTO {
        private Long id;
        private String title;
        private ExpenseCategory category;
        private BigDecimal amount;
        private LocalDateTime expenseDate;
    }
}