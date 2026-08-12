package com.smartpharma.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects excessive login/register attempts from the same IP before they reach the
 * authentication logic, to blunt brute-force/credential-stuffing attempts. Per-account
 * lockout (see AuthenticationServiceImpl) is the complementary, precise defense; this
 * filter is the coarse, IP-based one.
 */
@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginRateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean isRateLimited = path.endsWith("/api/auth/login") || path.endsWith("/api/auth/register");

        if (isRateLimited && !rateLimiter.tryAcquire(resolveClientIp(request))) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"Too many attempts. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Deliberately NOT trusting X-Forwarded-For here: there is currently no reverse
        // proxy in front of this app stripping/overwriting client-supplied headers, so
        // trusting it would let an attacker bypass rate limiting by randomizing the
        // header on every request. Switch to X-Forwarded-For (with a trusted-proxy check)
        // once a proxy/load balancer is actually deployed in front of the app.
        return request.getRemoteAddr();
    }
}
