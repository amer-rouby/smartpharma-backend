package com.smartpharma.dto.settings.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsRequest {

    private Boolean emailNotifications;
    private Boolean smsNotifications;
    private Boolean pushNotifications;
    private Boolean soundEnabled;
    private Boolean vibrationEnabled;
    private Boolean quietHoursEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;

    // Inventory
    private Boolean notifyLowStock;
    private Boolean notifyOutOfStock;
    private Boolean notifyExpiryWarning;
    private Boolean notifyExpiredProducts;

    // Sales
    private Boolean notifyNewSale;
    private Boolean notifyLargeSale;
    private Boolean notifyRefund;

    // Expenses
    private Boolean notifyNewExpense;
    private Boolean notifyLargeExpense;

    // System
    private Boolean notifySystemUpdates;
    private Boolean notifyBackupReminder;
    private Boolean notifySecurityAlerts;
}