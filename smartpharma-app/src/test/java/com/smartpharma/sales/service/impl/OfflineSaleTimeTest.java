package com.smartpharma.sales.service.impl;

import com.smartpharma.common.exception.LocalizedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfflineSaleTimeTest {

    private static final String CLIENT_ID = "8f14e45f-ceea-467a-9575-2f3e5d1c0a11";

    @Test
    void anOfflineSaleKeepsTheTimeItWasRungUp() {
        Instant soldAt = Instant.now().minus(Duration.ofHours(5));

        LocalDateTime recorded = SaleTransactionServiceImpl.resolveSaleTime(soldAt, CLIENT_ID);

        assertThat(recorded).isEqualTo(LocalDateTime.ofInstant(soldAt, ZoneId.systemDefault()));
    }

    @Test
    void theDeviceTimeIsIgnoredWithoutAClientSaleId() {
        LocalDateTime recorded = SaleTransactionServiceImpl.resolveSaleTime(
                Instant.now().minus(Duration.ofHours(5)), null);

        assertThat(Duration.between(recorded, LocalDateTime.now()).abs()).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void implausibleTimesAreRefused() {
        assertThatThrownBy(() -> SaleTransactionServiceImpl.resolveSaleTime(
                Instant.now().minus(Duration.ofHours(73)), CLIENT_ID))
                .isInstanceOf(LocalizedException.class).hasMessageContaining("72 hours");
        assertThatThrownBy(() -> SaleTransactionServiceImpl.resolveSaleTime(
                Instant.now().plus(Duration.ofMinutes(10)), CLIENT_ID))
                .isInstanceOf(LocalizedException.class);
        // A few minutes of clock skew between the till and the server is fine.
        assertThat(SaleTransactionServiceImpl.resolveSaleTime(Instant.now().plus(Duration.ofMinutes(2)), CLIENT_ID))
                .isNotNull();
    }
}
