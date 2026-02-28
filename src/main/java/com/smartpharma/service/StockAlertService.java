package com.smartpharma.service;

import com.smartpharma.dto.response.AlertStatsResponse;
import com.smartpharma.dto.response.StockAlertResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface StockAlertService {

    Page<StockAlertResponse> getAlerts(Long pharmacyId, int page, int size);

    Page<StockAlertResponse> getAlertsByStatus(Long pharmacyId, String status, int page, int size);

    List<StockAlertResponse> getActiveAlerts(Long pharmacyId);

    AlertStatsResponse getAlertStats(Long pharmacyId);

    void markAsRead(Long alertId, Long pharmacyId, Long userId);

    void markAllAsRead(Long pharmacyId, Long userId);

    void resolveAlert(Long alertId, Long pharmacyId, Long userId);

    void deleteAlert(Long alertId, Long pharmacyId);

    StockAlertResponse createAlert(Long pharmacyId,
                                   Long productId,
                                   Long batchId,
                                   com.smartpharma.entity.StockAlert.AlertType type,
                                   String title,
                                   String message,
                                   String severity);

    void generateLowStockAlerts(Long pharmacyId);
    void generateExpiryAlerts(Long pharmacyId);
}