package com.smartpharma.entity.settings;

import com.smartpharma.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_settings", schema = "smartpharma")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_notifications")
    @Builder.Default
    private Boolean emailNotifications = true;

    @Column(name = "sms_notifications")
    @Builder.Default
    private Boolean smsNotifications = false;

    @Column(name = "push_notifications")
    @Builder.Default
    private Boolean pushNotifications = true;

    @Column(name = "sound_enabled")
    @Builder.Default
    private Boolean soundEnabled = true;

    @Column(name = "vibration_enabled")
    @Builder.Default
    private Boolean vibrationEnabled = true;

    @Column(name = "quiet_hours_enabled")
    @Builder.Default
    private Boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start")
    private String quietHoursStart;

    @Column(name = "quiet_hours_end")
    private String quietHoursEnd;

    @Column(name = "notify_low_stock")
    @Builder.Default
    private Boolean notifyLowStock = true;

    @Column(name = "notify_out_of_stock")
    @Builder.Default
    private Boolean notifyOutOfStock = true;

    @Column(name = "notify_expiry_warning")
    @Builder.Default
    private Boolean notifyExpiryWarning = true;

    @Column(name = "notify_expired_products")
    @Builder.Default
    private Boolean notifyExpiredProducts = true;

    @Column(name = "notify_new_sale")
    @Builder.Default
    private Boolean notifyNewSale = false;

    @Column(name = "notify_large_sale")
    @Builder.Default
    private Boolean notifyLargeSale = true;

    @Column(name = "notify_refund")
    @Builder.Default
    private Boolean notifyRefund = true;

    @Column(name = "notify_new_expense")
    @Builder.Default
    private Boolean notifyNewExpense = true;

    @Column(name = "notify_large_expense")
    @Builder.Default
    private Boolean notifyLargeExpense = true;

    @Column(name = "notify_system_updates")
    @Builder.Default
    private Boolean notifySystemUpdates = true;

    @Column(name = "notify_backup_reminder")
    @Builder.Default
    private Boolean notifyBackupReminder = true;

    @Column(name = "notify_security_alerts")
    @Builder.Default
    private Boolean notifySecurityAlerts = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}