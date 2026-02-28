package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatsResponse {
    private Long totalAlerts;
    private Long unreadAlerts;
    private Long lowStockAlerts;
    private Long expiredAlerts;
    private Long expiringSoonAlerts;
    private Long outOfStockAlerts;
}