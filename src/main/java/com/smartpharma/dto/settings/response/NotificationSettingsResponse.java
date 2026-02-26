package com.smartpharma.dto.settings.response;

import com.smartpharma.entity.settings.NotificationSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsResponse {

    private Long id;
    private Long userId;
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

    private LocalDateTime updatedAt;

    public static NotificationSettingsResponse fromEntity(NotificationSettings settings) {
        return NotificationSettingsResponse.builder()
                .id(settings.getId())
                .userId(settings.getUser().getId())
                .emailNotifications(settings.getEmailNotifications())
                .smsNotifications(settings.getSmsNotifications())
                .pushNotifications(settings.getPushNotifications())
                .soundEnabled(settings.getSoundEnabled())
                .vibrationEnabled(settings.getVibrationEnabled())
                .quietHoursEnabled(settings.getQuietHoursEnabled())
                .quietHoursStart(settings.getQuietHoursStart())
                .quietHoursEnd(settings.getQuietHoursEnd())
                .notifyLowStock(settings.getNotifyLowStock())
                .notifyOutOfStock(settings.getNotifyOutOfStock())
                .notifyExpiryWarning(settings.getNotifyExpiryWarning())
                .notifyExpiredProducts(settings.getNotifyExpiredProducts())
                .notifyNewSale(settings.getNotifyNewSale())
                .notifyLargeSale(settings.getNotifyLargeSale())
                .notifyRefund(settings.getNotifyRefund())
                .notifyNewExpense(settings.getNotifyNewExpense())
                .notifyLargeExpense(settings.getNotifyLargeExpense())
                .notifySystemUpdates(settings.getNotifySystemUpdates())
                .notifyBackupReminder(settings.getNotifyBackupReminder())
                .notifySecurityAlerts(settings.getNotifySecurityAlerts())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}