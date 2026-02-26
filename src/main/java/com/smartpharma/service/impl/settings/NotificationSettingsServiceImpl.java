package com.smartpharma.service.impl.settings;


import com.smartpharma.dto.settings.request.NotificationSettingsRequest;
import com.smartpharma.dto.settings.response.NotificationSettingsResponse;
import com.smartpharma.entity.settings.NotificationSettings;
import com.smartpharma.entity.User;
import com.smartpharma.repository.settings.NotificationSettingsRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.service.settings.NotificationSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSettingsServiceImpl implements NotificationSettingsService {

    private final NotificationSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationSettingsResponse getSettings(Long userId) {
        NotificationSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        return NotificationSettingsResponse.fromEntity(settings);
    }

    @Override
    @Transactional
    public NotificationSettingsResponse updateSettings(Long userId, NotificationSettingsRequest request) {
        NotificationSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        // Update general settings
        if (request.getEmailNotifications() != null) {
            settings.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getSmsNotifications() != null) {
            settings.setSmsNotifications(request.getSmsNotifications());
        }
        if (request.getPushNotifications() != null) {
            settings.setPushNotifications(request.getPushNotifications());
        }
        if (request.getSoundEnabled() != null) {
            settings.setSoundEnabled(request.getSoundEnabled());
        }
        if (request.getVibrationEnabled() != null) {
            settings.setVibrationEnabled(request.getVibrationEnabled());
        }
        if (request.getQuietHoursEnabled() != null) {
            settings.setQuietHoursEnabled(request.getQuietHoursEnabled());
        }
        if (request.getQuietHoursStart() != null) {
            settings.setQuietHoursStart(request.getQuietHoursStart());
        }
        if (request.getQuietHoursEnd() != null) {
            settings.setQuietHoursEnd(request.getQuietHoursEnd());
        }

        // Update inventory notifications
        if (request.getNotifyLowStock() != null) {
            settings.setNotifyLowStock(request.getNotifyLowStock());
        }
        if (request.getNotifyOutOfStock() != null) {
            settings.setNotifyOutOfStock(request.getNotifyOutOfStock());
        }
        if (request.getNotifyExpiryWarning() != null) {
            settings.setNotifyExpiryWarning(request.getNotifyExpiryWarning());
        }
        if (request.getNotifyExpiredProducts() != null) {
            settings.setNotifyExpiredProducts(request.getNotifyExpiredProducts());
        }

        // Update sales notifications
        if (request.getNotifyNewSale() != null) {
            settings.setNotifyNewSale(request.getNotifyNewSale());
        }
        if (request.getNotifyLargeSale() != null) {
            settings.setNotifyLargeSale(request.getNotifyLargeSale());
        }
        if (request.getNotifyRefund() != null) {
            settings.setNotifyRefund(request.getNotifyRefund());
        }

        // Update expense notifications
        if (request.getNotifyNewExpense() != null) {
            settings.setNotifyNewExpense(request.getNotifyNewExpense());
        }
        if (request.getNotifyLargeExpense() != null) {
            settings.setNotifyLargeExpense(request.getNotifyLargeExpense());
        }

        // Update system notifications
        if (request.getNotifySystemUpdates() != null) {
            settings.setNotifySystemUpdates(request.getNotifySystemUpdates());
        }
        if (request.getNotifyBackupReminder() != null) {
            settings.setNotifyBackupReminder(request.getNotifyBackupReminder());
        }
        if (request.getNotifySecurityAlerts() != null) {
            settings.setNotifySecurityAlerts(request.getNotifySecurityAlerts());
        }

        settingsRepository.save(settings);
        log.info("Notification settings updated for userId: {}", userId);

        return NotificationSettingsResponse.fromEntity(settings);
    }

    private NotificationSettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return NotificationSettings.builder()
                .user(user)
                .emailNotifications(true)
                .smsNotifications(false)
                .pushNotifications(true)
                .soundEnabled(true)
                .vibrationEnabled(true)
                .quietHoursEnabled(false)
                .quietHoursStart("22:00")
                .quietHoursEnd("08:00")
                .notifyLowStock(true)
                .notifyOutOfStock(true)
                .notifyExpiryWarning(true)
                .notifyExpiredProducts(true)
                .notifyNewSale(false)
                .notifyLargeSale(true)
                .notifyRefund(true)
                .notifyNewExpense(true)
                .notifyLargeExpense(true)
                .notifySystemUpdates(true)
                .notifyBackupReminder(true)
                .notifySecurityAlerts(true)
                .build();
    }
}