package com.smartpharma.service.impl.settings;

import com.smartpharma.dto.settings.request.SecuritySettingsRequest;
import com.smartpharma.dto.settings.response.SecuritySettingsResponse;
import com.smartpharma.entity.settings.SecuritySettings;
import com.smartpharma.entity.User;
import com.smartpharma.repository.settings.SecuritySettingsRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.service.settings.SecuritySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecuritySettingsServiceImpl implements SecuritySettingsService {

    private final SecuritySettingsRepository securitySettingsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 30;

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