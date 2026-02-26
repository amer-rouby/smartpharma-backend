package com.smartpharma.controller.settings;


import com.smartpharma.dto.settings.request.NotificationSettingsRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.settings.response.NotificationSettingsResponse;
import com.smartpharma.service.settings.NotificationSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationSettingsController {

    private final NotificationSettingsService notificationSettingsService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getNotificationSettings(
            @RequestParam Long userId) {

        log.info("GET /api/settings/notifications - userId: {}", userId);

        NotificationSettingsResponse settings = notificationSettingsService.getSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(settings, "Notification settings retrieved successfully"));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateNotificationSettings(
            @RequestParam Long userId,
            @Valid @RequestBody NotificationSettingsRequest request) {

        log.info("PUT /api/settings/notifications - userId: {}", userId);

        NotificationSettingsResponse settings = notificationSettingsService.updateSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Notification settings updated successfully"));
    }
}