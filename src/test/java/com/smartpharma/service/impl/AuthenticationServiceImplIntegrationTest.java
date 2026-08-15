package com.smartpharma.service.impl;

import com.smartpharma.dto.request.LoginRequest;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.User;
import com.smartpharma.entity.settings.SecuritySettings;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.repository.settings.SecuritySettingsRepository;
import com.smartpharma.service.AuthenticationService;
import com.smartpharma.service.SessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression test for a real bug found while testing 2FA against a live instance:
 * SecuritySettingsServiceImpl.incrementFailedLoginAttempts() was never actually
 * persisting anything, because every caller re-throws right after calling it, and
 * with the default transaction propagation that re-thrown exception rolled back the
 * SAME transaction the increment had joined. A Mockito-based unit test of
 * SecuritySettingsServiceImpl in isolation cannot catch this class of bug at all -
 * transaction propagation is a container/proxy behavior that only shows up with a
 * real Spring transaction manager and a real database, which is exactly what this
 * test uses (full @SpringBootTest context, no mocks).
 */
@SpringBootTest
class AuthenticationServiceImplIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private PharmacyRepository pharmacyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SecuritySettingsRepository securitySettingsRepository;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Pharmacy pharmacy;
    private User user;

    @BeforeEach
    void setUp() {
        pharmacy = pharmacyRepository.save(Pharmacy.builder()
                .name("Auth Integration Test Pharmacy")
                .licenseNumber("AUTH-IT-" + System.nanoTime())
                .email("auth-it-" + System.nanoTime() + "@example.com")
                .subscriptionStatus(Pharmacy.SubscriptionStatus.ACTIVE)
                .planType(Pharmacy.PlanType.BASIC)
                .build());

        user = userRepository.save(User.builder()
                .pharmacy(pharmacy)
                .username("auth_it_user_" + System.nanoTime())
                .password(passwordEncoder.encode("CorrectPassword123!"))
                .fullName("Integration Test User")
                .role(User.UserRole.ADMIN)
                .isActive(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        // login() creates a real Session row via SessionService - must go before the
        // user, or the FK constraint rejects the delete (exactly what a live curl-based
        // manual test of this same flow ran into during the earlier 2FA/backup work).
        // Goes through SessionService (a real proxied singleton bean, @Transactional)
        // rather than the repository directly or a @Transactional test method - Spring's
        // test-method transaction support did not apply here in practice.
        sessionService.deleteAllUserSessions(user.getId());
        securitySettingsRepository.findByUserId(user.getId()).ifPresent(securitySettingsRepository::delete);
        userRepository.deleteById(user.getId());
        pharmacyRepository.deleteById(pharmacy.getId());
    }

    @Test
    void login_withWrongPassword_actuallyPersistsTheFailedAttempt() {
        LoginRequest badLogin = new LoginRequest(user.getUsername(), "WrongPassword123!", false);

        assertThatThrownBy(() -> authenticationService.login(badLogin))
                .isInstanceOf(Exception.class);

        // The real assertion: re-read from the database in a fresh call, after the
        // login() transaction has fully rolled back and completed. Before the
        // REQUIRES_NEW fix, this was 0 no matter how many times the block above ran.
        SecuritySettings settings = securitySettingsRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(settings.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void login_fiveWrongPasswords_locksTheAccount() {
        LoginRequest badLogin = new LoginRequest(user.getUsername(), "WrongPassword123!", false);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authenticationService.login(badLogin));
        }

        SecuritySettings settings = securitySettingsRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(settings.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(settings.getAccountLocked()).isTrue();
        assertThat(settings.getAccountLockedUntil()).isNotNull();
    }

    @Test
    void login_withCorrectPassword_resetsAnyPriorFailedAttempts() {
        LoginRequest badLogin = new LoginRequest(user.getUsername(), "WrongPassword123!", false);
        assertThatThrownBy(() -> authenticationService.login(badLogin));

        SecuritySettings afterFailure = securitySettingsRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(afterFailure.getFailedLoginAttempts()).isEqualTo(1);

        LoginRequest goodLogin = new LoginRequest(user.getUsername(), "CorrectPassword123!", false);
        authenticationService.login(goodLogin);

        SecuritySettings afterSuccess = securitySettingsRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(afterSuccess.getFailedLoginAttempts()).isEqualTo(0);
    }
}
