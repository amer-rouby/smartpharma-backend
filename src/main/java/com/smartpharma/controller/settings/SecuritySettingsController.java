package com.smartpharma.controller.settings;

import com.smartpharma.dto.settings.request.SecuritySettingsRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.settings.response.SecuritySettingsResponse;
import com.smartpharma.dto.settings.response.TwoFactorSetupResponse;
import com.smartpharma.service.settings.SecuritySettingsService;
import com.smartpharma.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings/security")
@RequiredArgsConstructor
@Slf4j
public class SecuritySettingsController {

    private final SecuritySettingsService securitySettingsService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> getSecuritySettings() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("GET /api/settings/security - userId: {}", userId);

        SecuritySettingsResponse settings = securitySettingsService.getSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(settings, "Security settings retrieved successfully"));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> updateSecuritySettings(
            @Valid @RequestBody SecuritySettingsRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("PUT /api/settings/security - userId: {}", userId);

        SecuritySettingsResponse settings = securitySettingsService.updateSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Security settings updated successfully"));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> changePassword(
            @RequestBody Map<String, String> passwordData) {
        Long userId = SecurityUtils.getCurrentUserId();
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

    @PostMapping("/2fa/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setupTwoFactor() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("POST /api/settings/security/2fa/setup - userId: {}", userId);

        TwoFactorSetupResponse response = securitySettingsService.setupTwoFactor(userId);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Scan the QR code with your authenticator app, then confirm with a code"));
    }

    @PostMapping("/2fa/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> verifyTwoFactor(
            @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("POST /api/settings/security/2fa/verify - userId: {}", userId);

        SecuritySettingsResponse settings =
                securitySettingsService.verifyAndEnableTwoFactor(userId, body.get("code"));
        return ResponseEntity.ok(ApiResponse.success(settings, "Two-factor authentication enabled"));
    }

    @PostMapping("/2fa/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SecuritySettingsResponse>> disableTwoFactor(
            @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("POST /api/settings/security/2fa/disable - userId: {}", userId);

        SecuritySettingsResponse settings = securitySettingsService.disableTwoFactor(userId, body.get("code"));
        return ResponseEntity.ok(ApiResponse.success(settings, "Two-factor authentication disabled"));
    }

    // These two act on another user's account (an admin unlocking/resetting a team
    // member), so - unlike the endpoints above - they keep userId as an explicit
    // target rather than deriving it from the caller. hasRole('ADMIN') plus the
    // same-pharmacy check in the service layer keep this from being exploitable
    // the way the client-supplied userId used to be here (see git history).
    @PostMapping("/unlock-account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockAccount(@RequestParam Long userId) {
        Long adminPharmacyId = SecurityUtils.getCurrentPharmacyId();
        log.info("POST /api/settings/security/unlock-account - userId: {}, requested by pharmacy: {}",
                userId, adminPharmacyId);

        securitySettingsService.unlockAccount(userId, adminPharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Account unlocked successfully"));
    }

    @PostMapping("/reset-failed-attempts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetFailedAttempts(@RequestParam Long userId) {
        Long adminPharmacyId = SecurityUtils.getCurrentPharmacyId();
        log.info("POST /api/settings/security/reset-failed-attempts - userId: {}, requested by pharmacy: {}",
                userId, adminPharmacyId);

        securitySettingsService.resetFailedLoginAttempts(userId, adminPharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Failed attempts reset successfully"));
    }
}
