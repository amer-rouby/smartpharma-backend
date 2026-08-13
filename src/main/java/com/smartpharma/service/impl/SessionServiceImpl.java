package com.smartpharma.service.impl;

import com.smartpharma.dto.response.ExtendSessionResponse;
import com.smartpharma.dto.response.SessionStatusResponse;
import com.smartpharma.entity.Session;
import com.smartpharma.entity.User;
import com.smartpharma.exception.MaxExtensionsReachedException;
import com.smartpharma.repository.SessionRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

    private static final List<Integer> ALLOWED_TIMEOUTS = Arrays.asList(15, 30, 60, 120, 240);
    private static final int DEFAULT_TIMEOUT_MINUTES = 30;

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Session createSession(User user, String token, int sessionTimeoutMinutes) {
        int timeout = ALLOWED_TIMEOUTS.contains(sessionTimeoutMinutes)
                ? sessionTimeoutMinutes
                : DEFAULT_TIMEOUT_MINUTES;

        int maxExtensions = user.getMaxExtensions() != null ? user.getMaxExtensions() : 3;
        user.setMaxExtensions(maxExtensions);
        user.setRemainingExtensions(maxExtensions);
        user.setSessionExtendedCount(0);
        user.setWarningThreshold(user.getWarningThreshold() != null ? user.getWarningThreshold() : 5);
        user.setSessionTimeout(timeout);
        userRepository.save(user);

        Session session = Session.builder()
                .user(user)
                .token(token)
                .sessionTimeoutMinutes(timeout)
                .expiresAt(LocalDateTime.now().plusMinutes(timeout))
                .revoked(false)
                .build();

        return sessionRepository.save(session);
    }

    @Override
    @Transactional
    public Session validateSession(String token) {
        Session session = sessionRepository.findByToken(token)
                .orElse(null);

        if (session == null || session.isExpired()) {
            return null;
        }

        LocalDateTime timeoutThreshold = session.getLastActivityAt()
                .plusMinutes(session.getSessionTimeoutMinutes());
        if (LocalDateTime.now().isAfter(timeoutThreshold)) {
            session.setRevoked(true);
            sessionRepository.save(session);
            return null;
        }

        return session;
    }

    @Override
    @Transactional
    public void revokeSession(String token) {
        sessionRepository.revokeByToken(token);
    }

    @Override
    @Transactional
    public void revokeAllUserSessions(Long userId) {
        sessionRepository.revokeAllByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteAllUserSessions(Long userId) {
        sessionRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void updateLastActivity(String token) {
        sessionRepository.updateLastActivity(token, LocalDateTime.now());
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 15 * * * *") // every hour, offset from the other hourly jobs
    public void cleanupExpiredSessions() {
        // Was defined but never wired to @Scheduled, so the sessions table grew
        // unbounded forever - every authenticated request does a lookup against it.
        sessionRepository.cleanupExpiredSessions(LocalDateTime.now());
        log.debug("Expired/revoked session cleanup run completed");
    }

    @Override
    public List<Integer> getAllowedTimeouts() {
        return ALLOWED_TIMEOUTS;
    }

    @Override
    @Transactional
    public Session getCurrentSession(Long userId) {
        // Find the most recent active session for the user
        return sessionRepository.findByUserIdAndRevokedFalseAndExpiresAtAfter(userId, LocalDateTime.now())
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public Session updateSessionTimeout(Long userId, Integer timeoutMinutes, LocalDateTime newExpiresAt) {
        Session session = getCurrentSession(userId);
        if (session != null) {
            session.setExpiresAt(newExpiresAt);
            session.setSessionTimeoutMinutes(timeoutMinutes);
            return sessionRepository.save(session);
        }
        return null;
    }

    @Override
    @Transactional
    public SessionStatusResponse getSessionStatus(String token) {
        Session session = sessionRepository.findByToken(token)
                .orElse(null);

        if (session == null || session.isExpired()) {
            return SessionStatusResponse.builder()
                    .isActive(false)
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        long remainingMinutes = ChronoUnit.MINUTES.between(now, session.getExpiresAt());
        User user = session.getUser();

        // Null-safe checks for all user fields
        int warningThreshold = user.getWarningThreshold() != null ? user.getWarningThreshold() : 5;
        int remainingExtensions = user.getRemainingExtensions() != null ? user.getRemainingExtensions() : 0;
        int sessionExtendedCount = user.getSessionExtendedCount() != null ? user.getSessionExtendedCount() : 0;
        int maxExtensions = user.getMaxExtensions() != null ? user.getMaxExtensions() : 3;

        boolean canExtend = remainingExtensions > 0 && sessionExtendedCount < maxExtensions;

        return SessionStatusResponse.builder()
                .isActive(true)
                .expiresAt(session.getExpiresAt().toString())
                .remainingMinutes(Math.max(0, remainingMinutes))
                .warningThreshold(warningThreshold)
                .canExtend(canExtend)
                .remainingExtensions(remainingExtensions)
                .build();
    }

    @Override
    @Transactional
    public ExtendSessionResponse extendSession(String token) {
        Session session = sessionRepository.findByToken(token)
                .orElse(null);

        if (session == null || session.isExpired()) {
            return ExtendSessionResponse.builder()
                    .success(false)
                    .message("Session not found or expired")
                    .build();
        }

        User user = session.getUser();

        // Null-safe checks for extension limits
        int remainingExtensions = user.getRemainingExtensions() != null ? user.getRemainingExtensions() : 0;
        int sessionExtendedCount = user.getSessionExtendedCount() != null ? user.getSessionExtendedCount() : 0;
        int maxExtensions = user.getMaxExtensions() != null ? user.getMaxExtensions() : 3;

        if (remainingExtensions <= 0) {
            return ExtendSessionResponse.builder()
                    .success(false)
                    .message("Maximum session extensions reached. Please login again.")
                    .build();
        }

        if (sessionExtendedCount >= maxExtensions) {
            return ExtendSessionResponse.builder()
                    .success(false)
                    .message("Maximum session extensions reached. Please login again.")
                    .build();
        }

        // Extend the session
        LocalDateTime newExpiresAt = LocalDateTime.now().plusMinutes(session.getSessionTimeoutMinutes());
        session.setExpiresAt(newExpiresAt);
        sessionRepository.save(session);

        // Update user counters (use null-safe values)
        user.setRemainingExtensions(remainingExtensions - 1);
        user.setSessionExtendedCount(sessionExtendedCount + 1);
        userRepository.save(user);

        return ExtendSessionResponse.builder()
                .success(true)
                .expiresAt(newExpiresAt.toString())
                .remainingExtensions(user.getRemainingExtensions())
                .message("Session extended successfully")
                .build();
    }
}
