package com.smartpharma.einvoice.service.impl;

import com.smartpharma.common.exception.LocalizedException;
import com.smartpharma.einvoice.entity.EInvoiceSubmission;
import com.smartpharma.einvoice.repository.EInvoiceSubmissionRepository;
import com.smartpharma.sales.event.SaleAmendingEvent;
import com.smartpharma.sales.event.SaleCancelledEvent;
import com.smartpharma.sales.event.SaleCompletedEvent;
import com.smartpharma.sales.event.SaleCreatingEvent;
import com.smartpharma.sales.event.SaleReturnedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Keeps ETA in step with sales: requires the buyer ID where ETA does, issues
// the receipt once a sale commits and a return receipt once a sale is
// cancelled or some of its items are returned, and refuses edits that would make an issued receipt disagree
// with the sale. Issuing runs after commit and
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onSaleReturned(SaleReturnedEvent event) {
        try {
            issuer.issuePartialReturn(event.saleReturnId(), event.pharmacyId(), false)
                    .filter(submission -> submission.isIssued() && submission.getPosDevice() != null)
                    .ifPresent(submission -> submitter.deliverDeviceAsync(submission.getPosDevice().getId()));
        } catch (RuntimeException e) {
            log.error("ETA return receipt issuance failed for return {} of sale {}: {}",
                    event.saleReturnId(), event.saleId(), e.getMessage(), e);
        }
    }

    // Synchronous: throwing here aborts the sale before it's saved. Checked at
    // the till rather than when the receipt is built, so the cashier can ask
    // the customer for their ID instead of the sale going through and its
    // receipt failing later.
    @EventListener
    void onSaleCreating(SaleCreatingEvent event) {
        if (event.totalAmount() == null
                || event.totalAmount().compareTo(EtaReceiptBuilder.BUYER_ID_THRESHOLD) < 0
                || !issuer.isEnabled(event.pharmacyId())) {
            return;
        }
        if (isBlank(event.buyerNationalId()) || isBlank(event.buyerName())) {
            throw new LocalizedException(HttpStatus.BAD_REQUEST, "BUYER_ID_REQUIRED",
                    "Sales of 150,000 EGP or more need the buyer's national ID and name for the ETA e-receipt");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
