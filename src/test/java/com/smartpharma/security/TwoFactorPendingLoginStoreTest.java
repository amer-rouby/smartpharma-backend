package com.smartpharma.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TwoFactorPendingLoginStoreTest {

    private final TwoFactorPendingLoginStore store = new TwoFactorPendingLoginStore();

    @Test
    void issueThenPeek_returnsTheSameUserIdWithoutConsuming() {
        String token = store.issue(42L);

        assertThat(store.peek(token)).isEqualTo(42L);
        // peek() must NOT consume - this is the exact bug found and fixed while
        // testing the 2FA login flow live: a mistyped code shouldn't force the user
        // back through username+password just because the token got deleted on the
        // first (failed) attempt.
        assertThat(store.peek(token)).isEqualTo(42L);
        assertThat(store.peek(token)).isEqualTo(42L);
    }

    @Test
    void invalidate_preventsFurtherUse() {
        String token = store.issue(42L);
        assertThat(store.peek(token)).isEqualTo(42L);

        store.invalidate(token);

        assertThat(store.peek(token)).isNull();
    }

    @Test
    void peek_returnsNullForUnknownToken() {
        assertThat(store.peek("this-token-was-never-issued")).isNull();
    }

    @Test
    void peek_returnsNullForNullToken() {
        assertThat(store.peek(null)).isNull();
    }

    @Test
    void issue_producesDifferentTokensEachTime() {
        String tokenA = store.issue(1L);
        String tokenB = store.issue(1L);

        assertThat(tokenA).isNotEqualTo(tokenB);
        // Both remain independently valid until explicitly invalidated.
        assertThat(store.peek(tokenA)).isEqualTo(1L);
        assertThat(store.peek(tokenB)).isEqualTo(1L);
    }

    @Test
    void invalidate_ofOneTokenDoesNotAffectAnotherForTheSameUser() {
        String tokenA = store.issue(7L);
        String tokenB = store.issue(7L);

        store.invalidate(tokenA);

        assertThat(store.peek(tokenA)).isNull();
        assertThat(store.peek(tokenB)).isEqualTo(7L);
    }
}
