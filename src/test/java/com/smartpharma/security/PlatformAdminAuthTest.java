package com.smartpharma.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class PlatformAdminAuthTest {

    private final PlatformAdminAuth auth = new PlatformAdminAuth();

    @Test
    void require_acceptsTheConfiguredKey() {
        ReflectionTestUtils.setField(auth, "expectedKey", "correct-secret");

        assertThatCode(() -> auth.require("correct-secret")).doesNotThrowAnyException();
    }

    @Test
    void require_rejectsWrongKey() {
        ReflectionTestUtils.setField(auth, "expectedKey", "correct-secret");

        assertThatThrownBy(() -> auth.require("wrong-secret"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void require_rejectsNullKey() {
        ReflectionTestUtils.setField(auth, "expectedKey", "correct-secret");

        assertThatThrownBy(() -> auth.require(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void require_failsClosedWhenNoKeyIsConfigured() {
        // The critical fail-safe: an operator who forgets to set PLATFORM_ADMIN_API_KEY
        // must end up with these endpoints refusing everyone, not silently open.
        ReflectionTestUtils.setField(auth, "expectedKey", "");

        assertThatThrownBy(() -> auth.require("anything"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> auth.require(""))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> auth.require(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void require_failsClosedWhenKeyIsBlank() {
        ReflectionTestUtils.setField(auth, "expectedKey", "   ");

        assertThatThrownBy(() -> auth.require("   "))
                .isInstanceOf(AccessDeniedException.class);
    }
}
