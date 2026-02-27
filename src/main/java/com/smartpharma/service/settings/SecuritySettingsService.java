package com.smartpharma.service.settings;


import com.smartpharma.dto.settings.request.SecuritySettingsRequest;
import com.smartpharma.dto.settings.response.SecuritySettingsResponse;

public interface SecuritySettingsService {

    SecuritySettingsResponse getSettings(Long userId);

    SecuritySettingsResponse updateSettings(Long userId, SecuritySettingsRequest request);

    SecuritySettingsResponse changePassword(Long userId, String oldPassword, String newPassword);

    void incrementFailedLoginAttempts(Long userId);

    void resetFailedLoginAttempts(Long userId);

    void lockAccount(Long userId);

    void unlockAccount(Long userId);
}