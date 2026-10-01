package com.smartpharma.sales.service.impl;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyNetTest {

    private static final LocalDate D1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate D2 = LocalDate.of(2026, 10, 2);

    @Test
    void refundsLowerTheDayTheyHappenOnEvenWithoutSales() {
        List<Object[]> sales = List.<Object[]>of(new Object[]{D1, new BigDecimal("100.00"), 3L});
        List<Object[]> refunds = List.of(new Object[]{D1, new BigDecimal("10.00")}, new Object[]{D2, new BigDecimal("5.50")});

        List<Object[]> merged = DailyNet.merge(sales, refunds, true);

        assertThat(merged).hasSize(2);
        assertThat(merged.get(0)).containsExactly(D1, new BigDecimal("90.00"), 3L);
        assertThat(merged.get(1)).containsExactly(D2, new BigDecimal("-5.50"), 0L);
        assertThat(sales.get(0)[1]).isEqualTo(new BigDecimal("100.00"));
    }
}
