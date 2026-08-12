package com.smartpharma.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory, per-IP fixed-window rate limiter for the login/register endpoints.
 * Deliberately dependency-free (no Redis/Bucket4j) since the app currently runs as a
 * single instance; if it's ever scaled horizontally this needs to move to a shared store.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS_PER_WINDOW = 10;
    private static final long WINDOW_MILLIS = 5 * 60 * 1000L; // 5 minutes

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private static final class Window {
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        final AtomicInteger count = new AtomicInteger(0);
    }

    /**
     * @return true if the request is allowed, false if the caller should be rejected (429).
     */
    public boolean tryAcquire(String key) {
        Window window = windows.computeIfAbsent(key, k -> new Window());

        long now = System.currentTimeMillis();
        long start = window.windowStart.get();
        if (now - start > WINDOW_MILLIS) {
            // Window expired - reset it (best-effort under race, doesn't need to be perfectly atomic).
            window.windowStart.set(now);
            window.count.set(0);
        }

        return window.count.incrementAndGet() <= MAX_ATTEMPTS_PER_WINDOW;
    }
}
