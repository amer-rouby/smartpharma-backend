package com.smartpharma.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds "password was correct, waiting on the TOTP code" state between the two
 * login requests. Deliberately NOT a JWT: a pending-2FA token must never be usable
 * as a real bearer token if it leaked or if JwtAuthenticationFilter's checks were
 * ever loosened, so this uses a completely separate, opaque, short-lived, in-memory
 * token that only the /api/auth/2fa/login endpoint understands. Same
 * single-instance caveat as LoginRateLimiter: fine for the app's current
 * single-node deployment, would need a shared store if horizontally scaled.
 */
@Component
public class TwoFactorPendingLoginStore {

    private static final long TTL_MILLIS = 5 * 60 * 1000L; // 5 minutes to enter the code

    private record Entry(Long userId, long expiresAt) {
    }

    private final ConcurrentHashMap<String, Entry> pending = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public String issue(Long userId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pending.put(token, new Entry(userId, System.currentTimeMillis() + TTL_MILLIS));
        return token;
    }

    /** Looks up the pending userId WITHOUT consuming the token, so a mistyped TOTP
     * code doesn't force the user back to square one (username+password again) - they
     * can just retry with the same token until it expires or they get it right. */
    public Long peek(String token) {
        if (token == null) {
            return null;
        }
        Entry entry = pending.get(token);
        if (entry == null || entry.expiresAt() < System.currentTimeMillis()) {
            pending.remove(token);
            return null;
        }
        return entry.userId();
    }

    /** Call only once the code has actually been verified, so the token can't be
     * replayed to complete a second login. */
    public void invalidate(String token) {
        pending.remove(token);
    }
}
