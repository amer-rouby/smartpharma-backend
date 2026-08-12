package com.smartpharma.service.impl;

import com.smartpharma.entity.Session;
import com.smartpharma.repository.SessionRepository;
import com.smartpharma.service.SessionValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionValidationServiceImpl implements SessionValidationService {

    private final SessionRepository sessionRepository;

    @Override
    @Transactional(readOnly = true)
    public Session validateAndRefreshSession(String token) {
        Session session = sessionRepository.findByTokenWithUser(token)
                .orElse(null);

        if (session == null) {
            log.debug("Session not found for token");
            return null;
        }

        if (session.isExpired()) {
            log.debug("Session expired for token");
            return null;
        }

        // Check timeout based on last activity
        LocalDateTime timeoutThreshold = session.getLastActivityAt()
                .plusMinutes(session.getSessionTimeoutMinutes());
        if (LocalDateTime.now().isAfter(timeoutThreshold)) {
            log.debug("Session timed out for token - last activity: {}, timeout: {} min",
                    session.getLastActivityAt(), session.getSessionTimeoutMinutes());
            return null;
        }

        // Update last activity time in the same transaction
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);

        return session;
    }
}