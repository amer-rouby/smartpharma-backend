package com.smartpharma.notification;

import com.smartpharma.expenses.event.ExpenseAddedEvent;
import com.smartpharma.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ExpenseAddedEventListener {

    private final NotificationService notificationService;

    @EventListener
    void onExpenseAdded(ExpenseAddedEvent event) {
        notificationService.notifyExpenseAdded(event.pharmacyId(), event.expenseId(), event.amount());
    }
}
