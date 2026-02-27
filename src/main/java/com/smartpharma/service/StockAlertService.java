package com.smartpharma.service;

import com.smartpharma.dto.response.AlertStatsResponse;
import com.smartpharma.dto.response.StockAlertResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface StockAlertService {

    // Get alerts with pagination
    Page<StockAlertResponse> getAlerts(Long pharmacyId, int page, int size);

    // Get alerts by status
    Page<StockAlertResponse> getAlertsByStatus(Long pharmacyId, String status, int page, int size);

    // Get active alerts
    List<StockAlertResponse> getActiveAlerts(Long pharmacyId);

    // Get alert stats
    AlertStatsResponse getAlertStats(Long pharmacyId);

    // Mark as read
    void markAsRead(Long alertId, Long pharmacyId, Long userId);

    // Mark all as read
    void markAllAsRead(Long pharmacyId, Long userId);

    // Resolve alert
    void resolveAlert(Long alertId, Long pharmacyId, Long userId);

    // Delete alert
    void deleteAlert(Long alertId, Long pharmacyId);

    // Create alert
    StockAlertResponse createAlert(Long pharmacyId,
                                   Long productId,
                                   Long batchId,
                                   com.smartpharma.entity.StockAlert.AlertType type,
                                   String title,
                                   String message,
                                   String severity);

    // Generate alerts automatically
    void generateLowStockAlerts(Long pharmacyId);
    void generateExpiryAlerts(Long pharmacyId);
}