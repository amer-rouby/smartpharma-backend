package com.smartpharma.sales.event;

// Published when part (or all) of a sale's items are returned.
public record SaleReturnedEvent(Long pharmacyId, Long saleId, Long saleReturnId) {
}
