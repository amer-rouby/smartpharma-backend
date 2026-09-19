package com.smartpharma.notification;

import com.smartpharma.catalog.event.StockChangedEvent;
import com.smartpharma.notifications.service.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
class StockChangedEventListener {

    private final NotificationStreamService notificationStreamService;

    @EventListener
    void onStockChanged(StockChangedEvent event) {
        notificationStreamService.notifyStockChanged(event.pharmacyId(),
                Map.of("changeType", event.changeType(), "batch", event.batch()));
    }
}
