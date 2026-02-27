package com.smartpharma.controller.settings;

import com.smartpharma.dto.settings.request.SecuritySettingsRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.settings.response.SecuritySettingsResponse;
import com.smartpharma.service.settings.SecuritySettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings/security")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class SecuritySettingsController {

    private final SecuritySettingsService securitySettingsService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> getSecuritySettings(
            @RequestParam Long userId) {

        log.info("GET /api/settings/security - userId: {}", userId);

        SecuritySettingsResponse settings = securitySettingsService.getSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(settings, "Security settings retrieved successfully"));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> updateSecuritySettings(
            @RequestParam Long userId,
            @Valid @RequestBody SecuritySettingsRequest request) {

        log.info("PUT /api/settings/security - userId: {}", userId);

        SecuritySettingsResponse settings = securitySettingsService.updateSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Security settings updated successfully"));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> changePassword(
            @RequestParam Long userId,
            @RequestBody Map<String, String> passwordData) {

        log.info("POST /api/settings/security/change-password - userId: {}", userId);

        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Old password and new password are required")
            );
        }

        SecuritySettingsResponse settings = securitySettingsService.changePassword(userId, oldPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success(settings, "Password changed successfully"));
    }

    @PostMapping("/unlock-account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> unlockAccount(@RequestParam Long userId) {
        log.info("POST /api/settings/security/unlock-account - userId: {}", userId);

        securitySettingsService.unlockAccount(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Account unlocked successfully"));
    }

    @PostMapping("/reset-failed-attempts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> resetFailedAttempts(@RequestParam Long userId) {
        log.info("POST /api/settings/security/reset-failed-attempts - userId: {}", userId);

        securitySettingsService.resetFailedLoginAttempts(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Failed attempts reset successfully"));
    }
}