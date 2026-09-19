package com.smartpharma.sales.event;

import java.math.BigDecimal;

public record SaleCompletedEvent(Long pharmacyId, Long saleId, BigDecimal totalAmount) {
}
