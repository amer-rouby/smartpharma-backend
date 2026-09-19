package com.smartpharma.notification;

import com.smartpharma.sales.event.SaleCompletedEvent;
import com.smartpharma.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SaleCompletedEventListener {

    private final NotificationService notificationService;

    @EventListener
    void onSaleCompleted(SaleCompletedEvent event) {
        notificationService.notifySaleCompleted(event.pharmacyId(), event.saleId(), event.totalAmount());
    }
}
