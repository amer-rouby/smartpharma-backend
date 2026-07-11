package com.smartpharma.util;

import com.smartpharma.entity.User;
import com.smartpharma.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

@Slf4j
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        if (userDetails instanceof User user) {
            return user.getId();
        }
        try {
            return Long.valueOf(userDetails.getUsername());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?> detailsMap) {
            Object userIdObj = detailsMap.get("userId");
            if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            }
            if (userIdObj instanceof Integer) {
                return ((Integer) userIdObj).longValue();
            }
            if (userIdObj instanceof String) {
                try {
                    return Long.valueOf((String) userIdObj);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }

        return null;
    }

    public static Long extractPharmacyId(UserDetails userDetails) {
        if (userDetails instanceof User user && user.getPharmacy() != null) {
            return user.getPharmacy().getId();
        }
        return null;
    }

    public static Long extractUserIdFromToken(String authHeader, JwtService jwtService) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                return jwtService.extractUserId(jwt);
            }
        } catch (Exception e) {
            log.warn("Could not extract userId from token", e);
        }
        return null;
    }
}
