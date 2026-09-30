package com.smartpharma.sales.service.impl;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SaleReturnDiscountShareTest {

    private static BigDecimal share(String discount, String subtotal, String items, String previous, boolean all) {
        return SaleReturnServiceImpl.discountShare(new BigDecimal(discount), new BigDecimal(subtotal),
                new BigDecimal(items), new BigDecimal(previous), all);
    }

    @Test
    void noDiscountMeansNoShare() {
        assertThat(share("0", "100.00", "40.00", "0", false)).isEqualByComparingTo("0");
    }

    @Test
    void theShareFollowsTheReturnedValue() {
        assertThat(share("10.00", "100.00", "40.00", "0", false)).isEqualByComparingTo("4.00");
        assertThat(share("1.00", "34.20", "11.40", "0", false)).isEqualByComparingTo("0.33");
    }

    @Test
    void theLastReturnTakesWhateverIsLeftSoSharesAddUp() {
        BigDecimal first = share("1.00", "34.20", "11.40", "0", false);
        BigDecimal second = share("1.00", "34.20", "11.40", first.toPlainString(), false);
        BigDecimal last = share("1.00", "34.20", "11.40", first.add(second).toPlainString(), true);

        assertThat(first.add(second).add(last)).isEqualByComparingTo("1.00");
        assertThat(last).isEqualByComparingTo("0.34");
    }
}
