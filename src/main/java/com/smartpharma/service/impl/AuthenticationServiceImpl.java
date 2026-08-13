package com.smartpharma.service.impl;

import com.smartpharma.dto.request.LoginRequest;
import com.smartpharma.dto.request.RegisterRequest;
import com.smartpharma.dto.response.AuthResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.User;
import com.smartpharma.exception.AccountLockedException;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.security.JwtService;
import com.smartpharma.security.TwoFactorPendingLoginStore;
import com.smartpharma.service.AuthenticationService;
import com.smartpharma.service.SessionService;
import com.smartpharma.service.settings.SecuritySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 30;

    private final UserRepository userRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SessionService sessionService;
    private final SecuritySettingsService securitySettingsService;
    private final TwoFactorPendingLoginStore twoFactorPendingLoginStore;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Pharmacy pharmacy;

        if (request.getPharmacyName() != null && !request.getPharmacyName().isBlank()) {
            if (request.getLicenseNumber() == null || request.getLicenseNumber().isBlank()) {
                throw new RuntimeException("License number is required for pharmacy registration");
            }

            pharmacy = Pharmacy.builder()
                    .name(request.getPharmacyName())
                    .licenseNumber(request.getLicenseNumber())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .subscriptionStatus(Pharmacy.SubscriptionStatus.ACTIVE)
                    .planType(Pharmacy.PlanType.BASIC)
                    .build();

            pharmacy = pharmacyRepository.save(pharmacy);
        } else if (request.getPharmacyId() != null) {
            pharmacy = pharmacyRepository.findById(request.getPharmacyId())
                    .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
        } else {
            throw new RuntimeException("Either pharmacyId or pharmacyName is required for registration");
        }

        // Usernames must be globally unique, not just per-pharmacy: login (findByUsername)
        // has no pharmacy context to disambiguate with, so two pharmacies sharing a
        // username would make login itself ambiguous/broken for both of them, not just
        // registration. UserServiceImpl (admin-created users) already checks this
        // correctly via existsByUsername; registration was checking the wrong scope.
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        String roleName;
        if (request.getPharmacyName() != null && !request.getPharmacyName().isBlank()) {
            roleName = "ADMIN";
        } else {
            roleName = (request.getRole() != null && !request.getRole().isBlank()) ? request.getRole() : "PHARMACIST";
        }

        User user = User.builder()
                .pharmacy(pharmacy)
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(User.UserRole.valueOf(roleName))
                .isActive(true)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user, pharmacy.getId());
        String refreshToken = jwtService.generateRefreshToken(user);
        long expiresInMs = jwtService.extractClaim(accessToken, claims -> claims.getExpiration().getTime());

        // Create session for the new user
        sessionService.createSession(user, accessToken, DEFAULT_SESSION_TIMEOUT_MINUTES);

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .pharmacyId(pharmacy.getId())
                .pharmacyName(pharmacy.getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInMs)
                .expiresAt(LocalDateTime.now().plusMinutes(DEFAULT_SESSION_TIMEOUT_MINUTES))
                .sessionTimeout(DEFAULT_SESSION_TIMEOUT_MINUTES)
                .warningThreshold(user.getWarningThreshold())
                .maxExtensions(user.getMaxExtensions())
                .remainingExtensions(user.getRemainingExtensions())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User existingUser = userRepository.findByUsername(request.getUsername()).orElse(null);

        if (existingUser != null) {
            Long remainingLockMinutes = securitySettingsService.getRemainingLockMinutesIfLocked(existingUser.getId());
            if (remainingLockMinutes != null) {
                throw new AccountLockedException(
                        "Account is locked due to too many failed login attempts. Try again in "
                                + remainingLockMinutes + " minute(s).");
            }
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            if (existingUser != null) {
                securitySettingsService.incrementFailedLoginAttempts(existingUser.getId());
            }
            throw ex;
        }

        // Reload user from database to get fresh data including session_timeout
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.getIsActive()) {
            throw new RuntimeException("User account is deactivated");
        }

        if (securitySettingsService.isTwoFactorEnabled(user.getId())) {
            // Password was correct, but that's only the first factor - don't reset the
            // failed-attempt counter or issue any real token yet. The pending token is
            // opaque and single-purpose (see TwoFactorPendingLoginStore); it cannot be
            // used to call any other authenticated endpoint.
            String tempToken = twoFactorPendingLoginStore.issue(user.getId());
            return AuthResponse.builder()
                    .twoFactorRequired(true)
                    .twoFactorTempToken(tempToken)
                    .build();
        }

        securitySettingsService.resetFailedLoginAttempts(user.getId());
        return completeLogin(user);
    }

    @Override
    @Transactional
    public AuthResponse completeTwoFactorLogin(String tempToken, String code) {
        Long userId = twoFactorPendingLoginStore.peek(tempToken);
        if (userId == null) {
            throw new RuntimeException("Two-factor login request expired or invalid - please log in again");
        }

        Long remainingLockMinutes = securitySettingsService.getRemainingLockMinutesIfLocked(userId);
        if (remainingLockMinutes != null) {
            twoFactorPendingLoginStore.invalidate(tempToken);
            throw new AccountLockedException(
                    "Account is locked due to too many failed attempts. Try again in "
                            + remainingLockMinutes + " minute(s).");
        }

        if (!securitySettingsService.verifyTwoFactorCode(userId, code)) {
            securitySettingsService.incrementFailedLoginAttempts(userId);
            // Deliberately NOT invalidating the token here - a mistyped code shouldn't
            // force the user back through username+password too, only exhausting the
            // account-lockout attempt count (or the token's own TTL) should.
            throw new RuntimeException("Invalid two-factor code");
        }

        twoFactorPendingLoginStore.invalidate(tempToken);
        securitySettingsService.resetFailedLoginAttempts(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!user.getIsActive()) {
            throw new RuntimeException("User account is deactivated");
        }

        return completeLogin(user);
    }

    /** Shared tail end of both the plain and two-factor login paths: attribute the
     * login, revoke old sessions, issue tokens, create the new session. */
    private AuthResponse completeLogin(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Revoke any existing sessions for this user
        sessionService.revokeAllUserSessions(user.getId());

        String accessToken = jwtService.generateToken(user, user.getPharmacy().getId());
        String refreshToken = jwtService.generateRefreshToken(user);
        long expiresInMs = jwtService.extractClaim(accessToken, claims -> claims.getExpiration().getTime());

        // Get fresh user data from database to ensure we have the latest session_timeout
        User freshUser = userRepository.findById(user.getId()).orElse(user);
        int sessionTimeout = freshUser.getSessionTimeout() != null ? freshUser.getSessionTimeout() : DEFAULT_SESSION_TIMEOUT_MINUTES;
        sessionService.createSession(freshUser, accessToken, sessionTimeout);

        return AuthResponse.builder()
                .userId(freshUser.getId())
                .username(freshUser.getUsername())
                .fullName(freshUser.getFullName())
                .role(freshUser.getRole().name())
                .pharmacyId(freshUser.getPharmacy().getId())
                .pharmacyName(freshUser.getPharmacy().getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInMs)
                .expiresAt(LocalDateTime.now().plusMinutes(sessionTimeout))
                .sessionTimeout(sessionTimeout)
                .warningThreshold(freshUser.getWarningThreshold())
                .maxExtensions(freshUser.getMaxExtensions())
                .remainingExtensions(freshUser.getRemainingExtensions())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Revoke old sessions and create new one
        sessionService.revokeAllUserSessions(user.getId());

        // Get fresh user data from database
        User freshUser = userRepository.findById(user.getId()).orElse(user);
        int sessionTimeout = freshUser.getSessionTimeout() != null ? freshUser.getSessionTimeout() : DEFAULT_SESSION_TIMEOUT_MINUTES;

        String newAccessToken = jwtService.generateToken(freshUser, freshUser.getPharmacy().getId());
        long expiresInMs = jwtService.extractClaim(newAccessToken, claims -> claims.getExpiration().getTime());

        // Create new session with user's current timeout setting
        sessionService.createSession(freshUser, newAccessToken, sessionTimeout);

        return AuthResponse.builder()
                .userId(freshUser.getId())
                .username(freshUser.getUsername())
                .fullName(freshUser.getFullName())
                .role(freshUser.getRole().name())
                .pharmacyId(freshUser.getPharmacy().getId())
                .pharmacyName(freshUser.getPharmacy().getName())
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInMs)
                .expiresAt(LocalDateTime.now().plusMinutes(sessionTimeout))
                .sessionTimeout(sessionTimeout)
                .warningThreshold(freshUser.getWarningThreshold())
                .maxExtensions(freshUser.getMaxExtensions())
                .remainingExtensions(freshUser.getRemainingExtensions())
                .build();
    }

    @Override
    @Transactional
    public void logout(String accessToken) {
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
        sessionService.revokeSession(accessToken);
    }
}