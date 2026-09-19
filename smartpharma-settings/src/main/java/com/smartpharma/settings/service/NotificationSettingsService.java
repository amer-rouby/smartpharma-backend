package com.smartpharma.settings.service;

import com.smartpharma.settings.dto.request.NotificationSettingsRequest;
import com.smartpharma.settings.dto.response.NotificationSettingsResponse;

public interface NotificationSettingsService {

    NotificationSettingsResponse getSettings(Long userId);

    NotificationSettingsResponse updateSettings(Long userId, NotificationSettingsRequest request);
}