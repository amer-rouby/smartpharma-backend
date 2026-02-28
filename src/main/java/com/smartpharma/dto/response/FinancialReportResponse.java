package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialReportResponse {
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;
    private BigDecimal profitMargin;
    private List<MonthlyFinancialDTO> monthlyData;
    private List<CategoryExpenseDTO> expensesByCategory;

    @Data
    @Builder
    public static class MonthlyFinancialDTO {
        private String month;
        private BigDecimal revenue;
        private BigDecimal expenses;
        private BigDecimal profit;
    }

    @Data
    @Builder
    public static class CategoryExpenseDTO {
        private String category;
        private BigDecimal amount;
    }
}