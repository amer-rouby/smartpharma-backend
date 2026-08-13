package com.smartpharma.service.settings;


import com.smartpharma.dto.settings.request.SecuritySettingsRequest;
import com.smartpharma.dto.settings.response.SecuritySettingsResponse;
import com.smartpharma.dto.settings.response.TwoFactorSetupResponse;

public interface SecuritySettingsService {

    SecuritySettingsResponse getSettings(Long userId);

    /** Generates and stores a new TOTP secret for the user, NOT yet enabled - the
     * caller must confirm it with verifyAndEnableTwoFactor before it takes effect. */
    TwoFactorSetupResponse setupTwoFactor(Long userId);

    /** Confirms setupTwoFactor's secret with a real code from the user's authenticator
     * app and, if valid, actually turns 2FA on. */
    SecuritySettingsResponse verifyAndEnableTwoFactor(Long userId, String code);

    /** Requires a valid current TOTP code (not just a toggle) to turn 2FA back off,
     * so a stolen session alone can't disable it. */
    SecuritySettingsResponse disableTwoFactor(Long userId, String code);

    boolean isTwoFactorEnabled(Long userId);

    /** Used at login time, after password auth succeeds, to check the second factor. */
    boolean verifyTwoFactorCode(Long userId, String code);

    SecuritySettingsResponse updateSettings(Long userId, SecuritySettingsRequest request);

    SecuritySettingsResponse changePassword(Long userId, String oldPassword, String newPassword);

    void incrementFailedLoginAttempts(Long userId);

    void resetFailedLoginAttempts(Long userId);

    void lockAccount(Long userId);

    void unlockAccount(Long userId);

    /**
     * Admin-initiated variants: verify the target user belongs to
     * requestingAdminPharmacyId before acting, so an admin can only unlock/reset
     * users in their own pharmacy.
     */
    void unlockAccount(Long userId, Long requestingAdminPharmacyId);

    void resetFailedLoginAttempts(Long userId, Long requestingAdminPharmacyId);

    /**
     * Returns remaining lock time in minutes if the account is currently locked,
     * or null if it isn't (auto-unlocking it first if a previous lock has expired).
     */
    Long getRemainingLockMinutesIfLocked(Long userId);
}