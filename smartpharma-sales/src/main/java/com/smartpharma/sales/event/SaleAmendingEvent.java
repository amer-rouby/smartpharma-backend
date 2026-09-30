package com.smartpharma.sales.event;

// Published synchronously, before an update that changes what a customer was
// charged or how (discount, payment method, customer phone). A listener may
// veto the update by throwing - e.g. once a tax receipt was issued for the
// sale, it can only be corrected by returning it and selling again.
public record SaleAmendingEvent(Long pharmacyId, Long saleId) {
}
