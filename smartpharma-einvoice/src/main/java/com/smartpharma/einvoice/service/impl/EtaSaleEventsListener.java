package com.smartpharma.einvoice.service.impl;

import com.smartpharma.common.exception.LocalizedException;
import com.smartpharma.einvoice.entity.EInvoiceSubmission;
import com.smartpharma.einvoice.repository.EInvoiceSubmissionRepository;
import com.smartpharma.sales.event.SaleAmendingEvent;
import com.smartpharma.sales.event.SaleCancelledEvent;
import com.smartpharma.sales.event.SaleCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Keeps ETA in step with sales: issues the receipt once a sale commits, the
// return receipt once a sale is cancelled, and refuses edits that would make
// an issued receipt disagree with the sale. Issuing runs after commit and
// never throws - a failure would otherwise surface as an error on a sale that
// already succeeded - so problems are logged and left for the retry timer.
@Component
@RequiredArgsConstructor
@Slf4j
class EtaSaleEventsListener {

    private final EtaReceiptIssuer issuer;
    private final EtaReceiptSubmitter submitter;
    private final EInvoiceSubmissionRepository submissionRepository;

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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onSaleCancelled(SaleCancelledEvent event) {
        try {
            issuer.issueReturn(event.saleId(), event.pharmacyId(), false)
                    .filter(submission -> submission.isIssued() && submission.getPosDevice() != null)
                    .ifPresent(submission -> submitter.deliverDeviceAsync(submission.getPosDevice().getId()));
        } catch (RuntimeException e) {
            log.error("ETA return receipt issuance failed for sale {}: {}", event.saleId(), e.getMessage(), e);
        }
    }

    // Synchronous: throwing here aborts the sale update. Once ETA has (or will
    // have) the receipt, changing the discount, payment method or buyer would
    // leave it describing a different sale; the ETA-correct fix is to cancel
    // the sale (which issues a return receipt) and sell again.
    @EventListener
    void onSaleAmending(SaleAmendingEvent event) {
        submissionRepository.findBySaleTransactionId(event.saleId())
                .filter(EInvoiceSubmission::isIssued)
                .filter(submission -> submission.getStatus() != EInvoiceSubmission.Status.REJECTED)
                .ifPresent(submission -> {
                    throw new LocalizedException(HttpStatus.CONFLICT, "SALE_HAS_ETA_RECEIPT",
                            "This sale already has an ETA e-receipt - cancel it and sell again instead of editing it");
                });
    }
}
