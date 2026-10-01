package com.smartpharma.sales.service;

import java.time.LocalDateTime;
import java.util.List;

public interface SalesRevenueService {

    // [day, sales minus the refunds made that day, orders], by day. A refund
    // counts on the day of the return, so a day with only refunds is negative.
    List<Object[]> getDailySales(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate);
}
