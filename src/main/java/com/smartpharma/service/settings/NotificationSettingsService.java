package com.smartpharma.service.settings;

import com.smartpharma.dto.settings.request.NotificationSettingsRequest;
import com.smartpharma.dto.settings.response.NotificationSettingsResponse;

public interface NotificationSettingsService {

    NotificationSettingsResponse getSettings(Long userId);

    NotificationSettingsResponse updateSettings(Long userId, NotificationSettingsRequest request);
}