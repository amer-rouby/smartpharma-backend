package com.smartpharma.notification;

import com.smartpharma.settings.event.SecurityAlertEvent;
import com.smartpharma.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SecurityAlertEventListener {

    private final NotificationService notificationService;

    @EventListener
    void onSecurityAlert(SecurityAlertEvent event) {
        notificationService.notifySecurityAlert(event.userId());
    }
}
