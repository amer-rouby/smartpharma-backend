package com.smartpharma.service;

import com.smartpharma.dto.request.NotificationRequest;
import com.smartpharma.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import java.util.List;

public interface NotificationService {

    NotificationResponse createNotification(NotificationRequest request);
    NotificationResponse markAsRead(Long notificationId, Long userId);

    int markAllAsRead(Long pharmacyId, Long userId);

    Page<NotificationResponse> getUserNotifications(Long pharmacyId, Long userId, int page, int size);
    List<NotificationResponse> getUnreadNotifications(Long pharmacyId, Long userId);
    Long getUnreadCount(Long pharmacyId, Long userId);

    void deleteNotification(Long notificationId, Long userId);

    void checkAndCreateLowStockAlerts(Long pharmacyId);
    void checkAndCreateExpiryAlerts(Long pharmacyId);
}