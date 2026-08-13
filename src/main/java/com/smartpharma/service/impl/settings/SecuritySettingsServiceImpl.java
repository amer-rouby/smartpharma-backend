package com.smartpharma.service.impl.settings;

import com.smartpharma.dto.settings.request.SecuritySettingsRequest;
import com.smartpharma.dto.settings.response.SecuritySettingsResponse;
import com.smartpharma.dto.settings.response.TwoFactorSetupResponse;
import com.smartpharma.entity.settings.SecuritySettings;
import com.smartpharma.entity.User;
import com.smartpharma.repository.settings.SecuritySettingsRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.security.TotpService;
import com.smartpharma.service.settings.SecuritySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecuritySettingsServiceImpl implements SecuritySettingsService {

    private final SecuritySettingsRepository securitySettingsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 30;
    private static final String TOTP_ISSUER = "SmartPharma";

    @Override
    @Transactional(readOnly = true)
    public SecuritySettingsResponse getSettings(Long userId) {
        SecuritySettings settings = securitySettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        return SecuritySettingsResponse.fromEntity(settings);
    }

    @Override
    @Transactional
    public SecuritySettingsResponse updateSettings(Long userId, SecuritySettingsRequest request) {
        SecuritySettings settings = securitySettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        // Update settings
        if (request.getTwoFactorEnabled() != null) {
            settings.setTwoFactorEnabled(request.getTwoFactorEnabled());
        }

        if (request.getSessionTimeoutMinutes() != null) {
            settings.setSessionTimeoutMinutes(request.getSessionTimeoutMinutes());
        }

        if (request.getRequirePasswordChange() != null) {
            settings.setRequirePasswordChange(request.getRequirePasswordChange());
        }

        if (request.getSecurityQuestion() != null) {
            settings.setSecurityQuestion(request.getSecurityQuestion());
        }

        if (request.getSecurityAnswer() != null) {
            settings.setSecurityAnswerHash(passwordEncoder.encode(request.getSecurityAnswer()));
        }

        if (request.getLoginHistoryEnabled() != null) {
            settings.setLoginHistoryEnabled(request.getLoginHistoryEnabled());
        }

        securitySettingsRepository.save(settings);
        log.info("Security settings updated for userId: {}", userId);

        return SecuritySettingsResponse.fromEntity(settings);
    }

    @Override
    @Transactional
    public SecuritySettingsResponse changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        SecuritySettings settings = securitySettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        settings.setLastPasswordChange(LocalDateTime.now());
        settings.setRequirePasswordChange(false);
        securitySettingsRepository.save(settings);

        log.info("Password changed for userId: {}", userId);
        return SecuritySettingsResponse.fromEntity(settings);
    }

    @Override
    @Transactional
    public TwoFactorSetupResponse setupTwoFactor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SecuritySettings settings = securitySettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        // A fresh secret on every setup call - if a previous setup was never
        // confirmed, this replaces it rather than leaving two live secrets around.
        String secret = totpService.generateSecret();
        settings.setTwoFactorSecret(secret);
        settings.setTwoFactorEnabled(false);
        securitySettingsRepository.save(settings);

        log.info("2FA setup started for userId: {}", userId);

        return TwoFactorSetupResponse.builder()
                .secret(secret)
                .otpAuthUrl(totpService.buildOtpAuthUrl(secret, user.getUsername(), TOTP_ISSUER))
                .build();
    }

    @Override
    @Transactional
    public SecuritySettingsResponse verifyAndEnableTwoFactor(Long userId, String code) {
        SecuritySettings settings = securitySettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Call 2FA setup first"));

        if (settings.getTwoFactorSecret() == null) {
            throw new RuntimeException("Call 2FA setup first");
        }
        if (!totpService.verifyCode(settings.getTwoFactorSecret(), code)) {
            throw new RuntimeException("Invalid verification code");
        }

        settings.setTwoFactorEnabled(true);
        securitySettingsRepository.save(settings);
        log.info("2FA enabled for userId: {}", userId);

        return SecuritySettingsResponse.fromEntity(settings);
    }

    @Override
    @Transactional
    public SecuritySettingsResponse disableTwoFactor(Long userId, String code) {
        SecuritySettings settings = securitySettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Security settings not found"));

        if (!Boolean.TRUE.equals(settings.getTwoFactorEnabled()) || settings.getTwoFactorSecret() == null) {
            throw new RuntimeException("Two-factor authentication is not enabled");
        }
        if (!totpService.verifyCode(settings.getTwoFactorSecret(), code)) {
            throw new RuntimeException("Invalid verification code");
        }

        settings.setTwoFactorEnabled(false);
        settings.setTwoFactorSecret(null);
        securitySettingsRepository.save(settings);
        log.info("2FA disabled for userId: {}", userId);

        return SecuritySettingsResponse.fromEntity(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTwoFactorEnabled(Long userId) {
        return securitySettingsRepository.findByUserId(userId)
                .map(SecuritySettings::getTwoFactorEnabled)
                .map(Boolean.TRUE::equals)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyTwoFactorCode(Long userId, String code) {
        return securitySettingsRepository.findByUserId(userId)
                .map(SecuritySettings::getTwoFactorSecret)
                .filter(secret -> secret != null)
                .map(secret -> totpService.verifyCode(secret, code))
                .orElse(false);
    }

    // REQUIRES_NEW is essential here, not just defensive: every caller of this method
    // (AuthenticationServiceImpl.login/completeTwoFactorLogin) immediately re-throws an
    // exception afterward to reject the request. With the default propagation, that
    // exception rolls back the CALLER's transaction - which this write would otherwise
    // be joined to - undoing the increment before it's ever committed. Verified this was
    // happening (failed_login_attempts stayed at 0 in the database no matter how many
    // wrong codes/passwords were submitted) before adding this annotation.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementFailedLoginAttempts(Long userId) {
        SecuritySettings settings = securitySettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        int attempts = settings.getFailedLoginAttempts() + 1;
        settings.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            lockAccount(userId);
        }

        securitySettingsRepository.save(settings);
        log.warn("Failed login attempt {} for userId: {}", attempts, userId);
    }

    @Override
    @Transactional
    public void resetFailedLoginAttempts(Long userId) {
        SecuritySettings settings = securitySettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        settings.setFailedLoginAttempts(0);
        settings.setAccountLocked(false);
        settings.setAccountLockedUntil(null);
        securitySettingsRepository.save(settings);

        log.info("Failed login attempts reset for userId: {}", userId);
    }

    @Override
    @Transactional
    public void lockAccount(Long userId) {
        SecuritySettings settings = securitySettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        settings.setAccountLocked(true);
        settings.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES));
        securitySettingsRepository.save(settings);

        log.warn("Account locked for userId: {} until: {}", userId, settings.getAccountLockedUntil());
    }

    @Override
    @Transactional
    public void unlockAccount(Long userId) {
        SecuritySettings settings = securitySettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        settings.setAccountLocked(false);
        settings.setAccountLockedUntil(null);
        settings.setFailedLoginAttempts(0);
        securitySettingsRepository.save(settings);

        log.info("Account unlocked for userId: {}", userId);
    }

    @Override
    @Transactional
    public void unlockAccount(Long userId, Long requestingAdminPharmacyId) {
        assertSamePharmacy(userId, requestingAdminPharmacyId);
        unlockAccount(userId);
    }

    @Override
    @Transactional
    public void resetFailedLoginAttempts(Long userId, Long requestingAdminPharmacyId) {
        assertSamePharmacy(userId, requestingAdminPharmacyId);
        resetFailedLoginAttempts(userId);
    }

    private void assertSamePharmacy(Long targetUserId, Long requestingAdminPharmacyId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (targetUser.getPharmacy() == null || !targetUser.getPharmacy().getId().equals(requestingAdminPharmacyId)) {
            throw new RuntimeException("User not found");
        }
    }

    @Override
    @Transactional
    public Long getRemainingLockMinutesIfLocked(Long userId) {
        SecuritySettings settings = securitySettingsRepository.findByUserId(userId).orElse(null);
        if (settings == null || !Boolean.TRUE.equals(settings.getAccountLocked())) {
            return null;
        }

        LocalDateTime lockedUntil = settings.getAccountLockedUntil();
        if (lockedUntil == null || !lockedUntil.isAfter(LocalDateTime.now())) {
            // Lock has expired (or has no expiry set) - auto-unlock instead of blocking forever.
            settings.setAccountLocked(false);
            settings.setAccountLockedUntil(null);
            settings.setFailedLoginAttempts(0);
            securitySettingsRepository.save(settings);
            return null;
        }

        return java.time.Duration.between(LocalDateTime.now(), lockedUntil).toMinutes() + 1;
    }

    private SecuritySettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return SecuritySettings.builder()
                .user(user)
                .twoFactorEnabled(false)
                .sessionTimeoutMinutes(30)
                .requirePasswordChange(false)
                .failedLoginAttempts(0)
                .accountLocked(false)
                .loginHistoryEnabled(true)
                .build();
    }
}