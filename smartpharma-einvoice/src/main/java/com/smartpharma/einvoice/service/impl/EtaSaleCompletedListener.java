package com.smartpharma.einvoice.service.impl;

import com.smartpharma.sales.event.SaleCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Issues the ETA receipt once the sale is committed, then sends it in the
// background. Anything thrown here would surface as an error on a sale that
// already succeeded, so failures are logged and left for the retry timer.
@Component
@RequiredArgsConstructor
@Slf4j
class EtaSaleCompletedListener {

    private final EtaReceiptIssuer issuer;
    private final EtaReceiptSubmitter submitter;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onSaleCompleted(SaleCompletedEvent event) {
        try {
            issuer.issueAfterSale(event.saleId(), event.pharmacyId())
                    .filter(submission -> submission.isIssued() && submission.getPosDevice() != null)
                    .ifPresent(submission -> submitter.deliverDeviceAsync(submission.getPosDevice().getId()));
        } catch (RuntimeException e) {
            log.error("ETA receipt issuance failed for sale {}: {}", event.saleId(), e.getMessage(), e);
        }
    }
}
