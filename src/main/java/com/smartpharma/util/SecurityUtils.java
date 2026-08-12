package com.smartpharma.util;

import com.smartpharma.entity.User;
import com.smartpharma.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

@Slf4j
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Returns the pharmacyId of the currently authenticated user, derived from the
     * SecurityContext (populated server-side from the validated JWT/DB user), never
     * from a client-supplied request parameter or body field. Controllers must use
     * this instead of trusting a "pharmacyId" sent by the caller, to prevent one
     * tenant from reading/writing another tenant's data (cross-tenant IDOR).
     */
    public static Long getCurrentPharmacyId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new AccessDeniedException("No authenticated user in security context");
        }
        Long pharmacyId = extractPharmacyId(userDetails);
        if (pharmacyId == null) {
            throw new AccessDeniedException("Authenticated user is not associated with a pharmacy");
        }
        return pharmacyId;
    }

    /**
     * Returns the id of the currently authenticated user from the SecurityContext.
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new AccessDeniedException("No authenticated user in security context");
        }
        Long userId = extractUserId(userDetails);
        if (userId == null) {
            throw new AccessDeniedException("Could not resolve authenticated user id");
        }
        return userId;
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
