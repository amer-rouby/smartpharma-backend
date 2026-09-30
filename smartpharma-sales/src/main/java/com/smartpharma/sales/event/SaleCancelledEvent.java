package com.smartpharma.sales.event;

// Published when a sale is soft-deleted, i.e. cancelled after the fact.
public record SaleCancelledEvent(Long pharmacyId, Long saleId) {
}
