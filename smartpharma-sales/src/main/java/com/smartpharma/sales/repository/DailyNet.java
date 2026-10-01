package com.smartpharma.sales.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Combines a per-day sales series with the refunds made on each day, so a
// refund lowers the day it happened on - including a day with no sales.
final class DailyNet {

    private DailyNet() {
    }

    /**
     * @param sales   rows [day, total] or, withOrders, [day, total, orders]
     * @param refunds rows [day, refunded]
     * @return the same row shape, total minus that day's refunds, by day
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static List<Object[]> merge(List<Object[]> sales, List<Object[]> refunds, boolean withOrders) {
        Map<Comparable, Object[]> byDay = new TreeMap<>();
        for (Object[] row : sales) {
            byDay.put((Comparable) row[0], row.clone());
        }
        for (Object[] refund : refunds) {
            Object[] row = byDay.computeIfAbsent((Comparable) refund[0], day -> withOrders
                    ? new Object[]{day, BigDecimal.ZERO, 0L}
                    : new Object[]{day, BigDecimal.ZERO});
            row[1] = ((BigDecimal) row[1]).subtract((BigDecimal) refund[1]);
        }
        return new ArrayList<>(byDay.values());
    }
}
