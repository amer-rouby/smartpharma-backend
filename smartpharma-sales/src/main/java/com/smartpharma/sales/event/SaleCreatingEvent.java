package com.smartpharma.sales.event;

import java.math.BigDecimal;

// Published synchronously once a new sale's total is known, before it's
// saved. A listener may refuse the sale by throwing - e.g. the e-receipt
// module requires the buyer's national ID from a threshold amount.
public record SaleCreatingEvent(Long pharmacyId, BigDecimal totalAmount, String buyerNationalId, String buyerName) {
}
