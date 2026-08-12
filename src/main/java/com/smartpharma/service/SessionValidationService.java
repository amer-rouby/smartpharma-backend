package com.smartpharma.service;

import com.smartpharma.entity.Session;
import com.smartpharma.entity.User;

/**
 * Service to validate sessions and provide user details in a single transaction.
 * This reduces the number of database connections needed per request.
 */
public interface SessionValidationService {

    /**
     * Validates a session token, updates last activity, and returns the session with user.
     * All operations happen in a single @Transactional boundary.
     *
     * @param token the session token
     * @return the validated session with user, or null if invalid/expired
     */
    Session validateAndRefreshSession(String token);
}