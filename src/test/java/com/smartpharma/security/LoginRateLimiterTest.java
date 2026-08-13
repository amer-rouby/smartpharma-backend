package com.smartpharma.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {

    private final LoginRateLimiter rateLimiter = new LoginRateLimiter();

    @Test
    void tryAcquire_allowsUpToTenRequestsThenBlocks() {
        String ip = "10.0.0.1";
        int allowed = 0;
        int blocked = 0;

        for (int i = 0; i < 15; i++) {
            if (rateLimiter.tryAcquire(ip)) {
                allowed++;
            } else {
                blocked++;
            }
        }

        assertThat(allowed).isEqualTo(10);
        assertThat(blocked).isEqualTo(5);
    }

    @Test
    void tryAcquire_tracksDifferentIpsIndependently() {
        String ipA = "10.0.0.2";
        String ipB = "10.0.0.3";

        for (int i = 0; i < 10; i++) {
            assertThat(rateLimiter.tryAcquire(ipA)).isTrue();
        }
        // ipA is now exhausted, but ipB should be completely unaffected.
        assertThat(rateLimiter.tryAcquire(ipA)).isFalse();
        assertThat(rateLimiter.tryAcquire(ipB)).isTrue();
    }
}
