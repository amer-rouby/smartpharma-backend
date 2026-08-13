package com.smartpharma.service.impl.settings;

import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.User;
import com.smartpharma.entity.settings.SecuritySettings;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.repository.settings.SecuritySettingsRepository;
import com.smartpharma.security.TotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecuritySettingsServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long PHARMACY_ID = 10L;
    private static final Long OTHER_PHARMACY_ID = 99L;

    @Mock
    private SecuritySettingsRepository securitySettingsRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TotpService totpService;

    private SecuritySettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SecuritySettingsServiceImpl(securitySettingsRepository, userRepository, passwordEncoder, totpService);
    }

    private User userIn(Long pharmacyId) {
        Pharmacy pharmacy = Pharmacy.builder().id(pharmacyId).build();
        return User.builder().id(USER_ID).pharmacy(pharmacy).username("testuser").build();
    }

    private SecuritySettings settingsWith(int failedAttempts) {
        return SecuritySettings.builder()
                .user(userIn(PHARMACY_ID))
                .failedLoginAttempts(failedAttempts)
                .accountLocked(false)
                .build();
    }

    @Test
    void incrementFailedLoginAttempts_below5_doesNotLock() {
        SecuritySettings settings = settingsWith(3);
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        service.incrementFailedLoginAttempts(USER_ID);

        assertThat(settings.getFailedLoginAttempts()).isEqualTo(4);
        assertThat(settings.getAccountLocked()).isFalse();
    }

    @Test
    void incrementFailedLoginAttempts_reaching5_locksTheAccount() {
        SecuritySettings settings = settingsWith(4);
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        service.incrementFailedLoginAttempts(USER_ID);

        assertThat(settings.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(settings.getAccountLocked()).isTrue();
        assertThat(settings.getAccountLockedUntil()).isAfter(LocalDateTime.now());
        // Roughly 30 minutes out - allow slack for test execution time.
        assertThat(settings.getAccountLockedUntil())
                .isBetween(LocalDateTime.now().plusMinutes(29), LocalDateTime.now().plusMinutes(31));
    }

    @Test
    void resetFailedLoginAttempts_clearsCountAndLock() {
        SecuritySettings settings = settingsWith(5);
        settings.setAccountLocked(true);
        settings.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        service.resetFailedLoginAttempts(USER_ID);

        assertThat(settings.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(settings.getAccountLocked()).isFalse();
        assertThat(settings.getAccountLockedUntil()).isNull();
    }

    @Test
    void getRemainingLockMinutesIfLocked_returnsNullWhenNotLocked() {
        SecuritySettings settings = settingsWith(0);
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        assertThat(service.getRemainingLockMinutesIfLocked(USER_ID)).isNull();
    }

    @Test
    void getRemainingLockMinutesIfLocked_returnsMinutesWhenLocked() {
        SecuritySettings settings = settingsWith(5);
        settings.setAccountLocked(true);
        settings.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        Long remaining = service.getRemainingLockMinutesIfLocked(USER_ID);

        assertThat(remaining).isBetween(9L, 11L);
    }

    @Test
    void getRemainingLockMinutesIfLocked_autoUnlocksAnExpiredLock() {
        SecuritySettings settings = settingsWith(5);
        settings.setAccountLocked(true);
        settings.setAccountLockedUntil(LocalDateTime.now().minusMinutes(1)); // already expired
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        Long remaining = service.getRemainingLockMinutesIfLocked(USER_ID);

        assertThat(remaining).isNull();
        assertThat(settings.getAccountLocked()).isFalse();
        assertThat(settings.getFailedLoginAttempts()).isEqualTo(0);
        verify(securitySettingsRepository).save(settings);
    }

    @Test
    void verifyTwoFactorCode_delegatesToTotpServiceWithStoredSecret() {
        SecuritySettings settings = settingsWith(0);
        settings.setTwoFactorSecret("SECRET123");
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(totpService.verifyCode("SECRET123", "654321")).thenReturn(true);

        assertThat(service.verifyTwoFactorCode(USER_ID, "654321")).isTrue();
    }

    @Test
    void verifyTwoFactorCode_falseWhenNoSecretConfigured() {
        SecuritySettings settings = settingsWith(0); // twoFactorSecret is null
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        assertThat(service.verifyTwoFactorCode(USER_ID, "123456")).isFalse();
        verify(totpService, never()).verifyCode(any(), any());
    }

    @Test
    void verifyAndEnableTwoFactor_requiresSetupFirst() {
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyAndEnableTwoFactor(USER_ID, "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("setup");
    }

    @Test
    void verifyAndEnableTwoFactor_rejectsWrongCodeWithoutEnabling() {
        SecuritySettings settings = settingsWith(0);
        settings.setTwoFactorSecret("SECRET123");
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(totpService.verifyCode("SECRET123", "000000")).thenReturn(false);

        assertThatThrownBy(() -> service.verifyAndEnableTwoFactor(USER_ID, "000000"))
                .isInstanceOf(RuntimeException.class);

        assertThat(settings.getTwoFactorEnabled()).isFalse();
    }

    @Test
    void verifyAndEnableTwoFactor_enablesOnCorrectCode() {
        SecuritySettings settings = settingsWith(0);
        settings.setTwoFactorSecret("SECRET123");
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(totpService.verifyCode("SECRET123", "654321")).thenReturn(true);

        service.verifyAndEnableTwoFactor(USER_ID, "654321");

        assertThat(settings.getTwoFactorEnabled()).isTrue();
    }

    @Test
    void disableTwoFactor_requiresCurrentlyEnabled() {
        SecuritySettings settings = settingsWith(0); // twoFactorEnabled defaults false
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        assertThatThrownBy(() -> service.disableTwoFactor(USER_ID, "654321"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not enabled");
    }

    @Test
    void disableTwoFactor_clearsSecretOnCorrectCode() {
        SecuritySettings settings = settingsWith(0);
        settings.setTwoFactorEnabled(true);
        settings.setTwoFactorSecret("SECRET123");
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(totpService.verifyCode("SECRET123", "654321")).thenReturn(true);

        service.disableTwoFactor(USER_ID, "654321");

        assertThat(settings.getTwoFactorEnabled()).isFalse();
        assertThat(settings.getTwoFactorSecret()).isNull();
    }

    // --- Admin-acting-on-another-user overloads: cross-pharmacy protection ---
    // These exist because of a real IDOR fixed alongside the 2FA work: unlock/reset
    // used to trust a client-supplied userId with no ownership check at all.

    @Test
    void unlockAccount_withAdminPharmacy_rejectsUserFromADifferentPharmacy() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userIn(PHARMACY_ID)));

        assertThatThrownBy(() -> service.unlockAccount(USER_ID, OTHER_PHARMACY_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");

        verify(securitySettingsRepository, never()).save(any());
    }

    @Test
    void unlockAccount_withAdminPharmacy_allowsUserFromTheSamePharmacy() {
        SecuritySettings settings = settingsWith(5);
        settings.setAccountLocked(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userIn(PHARMACY_ID)));
        when(securitySettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));

        service.unlockAccount(USER_ID, PHARMACY_ID);

        assertThat(settings.getAccountLocked()).isFalse();
    }

    @Test
    void resetFailedLoginAttempts_withAdminPharmacy_rejectsUserFromADifferentPharmacy() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userIn(PHARMACY_ID)));

        assertThatThrownBy(() -> service.resetFailedLoginAttempts(USER_ID, OTHER_PHARMACY_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }
}
