package com.smartpharma.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Gate for platform-operator-only endpoints (whole-database backup/restore) that
 * must NOT be reachable by any pharmacy's regular ADMIN role, no matter how well
 * @PreAuthorize is configured elsewhere - this is a completely separate secret,
 * known only to whoever runs the platform (set via PLATFORM_ADMIN_API_KEY), not
 * tied to any pharmacy/user account at all. Checked explicitly in the controller
 * via a header rather than folded into the normal JWT/role system, so there's no
 * way for it to be granted to a tenant user by mistake.
 */
@Component
public class PlatformAdminAuth {

    public static final String HEADER_NAME = "X-Platform-Admin-Key";

    @Value("${platform.admin-api-key:}")
    private String expectedKey;

    public void require(String providedKey) {
        if (expectedKey == null || expectedKey.isBlank()) {
            // Fails closed: if the operator never configured a key, nobody can use
            // this - not even with a blank/empty header - rather than accidentally
            // leaving these endpoints open to everyone.
            throw new AccessDeniedException("Platform admin operations are not configured on this deployment");
        }
        if (providedKey == null || !constantTimeEquals(expectedKey, providedKey)) {
            throw new AccessDeniedException("Invalid or missing platform admin key");
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        // Avoids a timing side-channel on key comparison; MessageDigest.isEqual is the
        // JDK's constant-time comparator.
        return java.security.MessageDigest.isEqual(
                a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
