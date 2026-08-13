package com.smartpharma.service;

import com.smartpharma.dto.response.ExtendSessionResponse;
import com.smartpharma.dto.response.SessionStatusResponse;
import com.smartpharma.entity.Session;
import com.smartpharma.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionService {

    Session createSession(User user, String token, int sessionTimeoutMinutes);

    Session validateSession(String token);

    void revokeSession(String token);

    void revokeAllUserSessions(Long userId);

    /** Hard-deletes (not just revokes) every session row for a user - needed before a
     * user row itself can be deleted, since sessions.user_id is a FK. */
    void deleteAllUserSessions(Long userId);

    void updateLastActivity(String token);

    void cleanupExpiredSessions();

    List<Integer> getAllowedTimeouts();

    SessionStatusResponse getSessionStatus(String token);

    ExtendSessionResponse extendSession(String token);

    Session getCurrentSession(Long userId);

    Session updateSessionTimeout(Long userId, Integer timeoutMinutes, LocalDateTime newExpiresAt);
}
