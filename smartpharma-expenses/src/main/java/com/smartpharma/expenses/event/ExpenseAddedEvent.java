package com.smartpharma.expenses.event;

import java.math.BigDecimal;

public record ExpenseAddedEvent(Long pharmacyId, Long expenseId, BigDecimal amount) {
}
