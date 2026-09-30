package com.smartpharma.einvoice.service.impl;

import com.smartpharma.common.exception.LocalizedException;
import com.smartpharma.einvoice.entity.EInvoiceSubmission;
import com.smartpharma.einvoice.repository.EInvoiceSubmissionRepository;
import com.smartpharma.sales.event.SaleAmendingEvent;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EtaSaleEventsListenerTest {

    private final EInvoiceSubmissionRepository repository = mock(EInvoiceSubmissionRepository.class);
    private final EtaSaleEventsListener listener = new EtaSaleEventsListener(null, null, repository);

    @Test
    void refusesToAmendASaleWhoseReceiptWasIssued() {
        when(repository.findBySaleTransactionId(7L)).thenReturn(Optional.of(receipt(EInvoiceSubmission.Status.SUBMITTED, true)));

        assertThatThrownBy(() -> listener.onSaleAmending(new SaleAmendingEvent(1L, 7L)))
                .isInstanceOf(LocalizedException.class)
                .hasMessageContaining("cancel it and sell again");
    }

    @Test
    void allowsAmendingWhenThereIsNothingAtEtaToContradict() {
        when(repository.findBySaleTransactionId(1L)).thenReturn(Optional.empty());
        when(repository.findBySaleTransactionId(2L)).thenReturn(Optional.of(receipt(EInvoiceSubmission.Status.ERROR, false)));
        when(repository.findBySaleTransactionId(3L)).thenReturn(Optional.of(receipt(EInvoiceSubmission.Status.REJECTED, true)));

        assertThatCode(() -> listener.onSaleAmending(new SaleAmendingEvent(1L, 1L))).doesNotThrowAnyException();
        assertThatCode(() -> listener.onSaleAmending(new SaleAmendingEvent(1L, 2L))).doesNotThrowAnyException();
        assertThatCode(() -> listener.onSaleAmending(new SaleAmendingEvent(1L, 3L))).doesNotThrowAnyException();
    }

    private static EInvoiceSubmission receipt(EInvoiceSubmission.Status status, boolean issued) {
        return EInvoiceSubmission.builder()
                .status(status)
                .etaUuid(issued ? "a".repeat(64) : null)
                .receiptJson(issued ? "{}" : null)
                .build();
    }
}
