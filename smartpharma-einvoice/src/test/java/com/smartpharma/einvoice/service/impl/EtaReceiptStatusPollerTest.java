package com.smartpharma.einvoice.service.impl;

import com.smartpharma.einvoice.entity.EInvoiceSubmission;
import com.smartpharma.einvoice.repository.EInvoiceSubmissionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EtaReceiptStatusPollerTest {

    @Test
    void mapsEtaValidityOntoSubmittedReceipts() {
        EInvoiceSubmission valid = submitted("aaa");
        EInvoiceSubmission invalid = submitted("bbb");
        EInvoiceSubmission cancelled = submitted("ccc");
        EInvoiceSubmission notInResponse = submitted("ddd");
        EInvoiceSubmissionRepository repository = mock(EInvoiceSubmissionRepository.class);
        when(repository.findSubmittedBySubmissionUuid("SUB1")).thenReturn(List.of(valid, invalid, cancelled, notInResponse));
        EtaReceiptStatusPoller poller = new EtaReceiptStatusPoller(repository, null, null, null, null, null);

        poller.apply("SUB1", Map.of(
                "aaa", new EtaApiClient.ReceiptOutcome("aaa", "Valid", null),
                "bbb", new EtaApiClient.ReceiptOutcome("bbb", "Invalid", "Tax code wrong"),
                "ccc", new EtaApiClient.ReceiptOutcome("ccc", "Cancelled", null)));

        assertThat(valid.getStatus()).isEqualTo(EInvoiceSubmission.Status.ACCEPTED);
        assertThat(valid.getErrorMessage()).isNull();
        assertThat(invalid.getStatus()).isEqualTo(EInvoiceSubmission.Status.REJECTED);
        assertThat(invalid.getErrorMessage()).isEqualTo("Tax code wrong");
        assertThat(cancelled.getStatus()).isEqualTo(EInvoiceSubmission.Status.REJECTED);
        assertThat(cancelled.getErrorMessage()).contains("Cancelled");
        assertThat(notInResponse.getStatus()).isEqualTo(EInvoiceSubmission.Status.SUBMITTED);
    }

    private static EInvoiceSubmission submitted(String uuid) {
        return EInvoiceSubmission.builder()
                .etaUuid(uuid)
                .submissionUuid("SUB1")
                .status(EInvoiceSubmission.Status.SUBMITTED)
                .build();
    }
}
