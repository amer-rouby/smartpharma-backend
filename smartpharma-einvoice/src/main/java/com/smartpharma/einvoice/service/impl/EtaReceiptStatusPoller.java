package com.smartpharma.einvoice.service.impl;

import com.smartpharma.common.exception.LocalizedException;
import com.smartpharma.einvoice.entity.EInvoiceSubmission;
import com.smartpharma.einvoice.entity.EtaPosDevice;
import com.smartpharma.einvoice.repository.EInvoiceSubmissionRepository;
import com.smartpharma.einvoice.repository.EtaPosDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A 202 from the submission API only means ETA took the batch for
// processing; each receipt is validated afterwards and ends up Valid or
// Invalid. This reads that outcome back for SUBMITTED receipts:
// Valid -> ACCEPTED, Invalid/Cancelled -> REJECTED with ETA's reasons (the
// pharmacy can then fix the data and Retry, which re-issues the receipt).
// Like the submitter, no DB transaction is held while waiting on ETA.
@Service
@RequiredArgsConstructor
@Slf4j
public class EtaReceiptStatusPoller {

    private final EInvoiceSubmissionRepository submissionRepository;
    private final EtaPosDeviceRepository deviceRepository;
    private final EtaCredentialsResolver credentialsResolver;
    private final EtaSecretCipher cipher;
    private final EtaApiClient apiClient;
    private final TransactionTemplate tx;

    @Scheduled(initialDelayString = "${eta.status.initial-delay-ms:90000}",
            fixedDelayString = "${eta.status.interval-ms:300000}")
    public void pollAll() {
        if (!cipher.isConfigured()) {
            return;
        }
        List<Object[]> pending = tx.execute(s -> submissionRepository.findSubmissionsAwaitingValidation());
        for (Object[] row : pending == null ? List.<Object[]>of() : pending) {
            try {
                poll((Long) row[0], (String) row[1]);
            } catch (RuntimeException e) {
                log.error("ETA status poll failed for submission {}: {}", row[1], e.getMessage(), e);
            }
        }
    }

    public void poll(Long deviceId, String submissionUuid) {
        EtaApiClient.PosCredentials credentials = tx.execute(s -> {
            EtaPosDevice device = deviceRepository.findById(deviceId).orElse(null);
            if (device == null) {
                return null;
            }
            try {
                return credentialsResolver.resolve(device);
            } catch (LocalizedException e) {
                log.warn("Can't poll ETA submission {}: {}", submissionUuid, e.getMessage());
                return null;
            }
        });
        if (credentials == null) {
            return;
        }

        Map<String, EtaApiClient.ReceiptOutcome> outcomes = new HashMap<>();
        int totalPages = 1;
        for (int page = 1; page <= totalPages; page++) {
            EtaApiClient.SubmissionDetails details = apiClient.getSubmissionDetails(credentials, submissionUuid, page);
            if (details.failure() != null) {
                log.warn("ETA submission {} status not read: {}", submissionUuid, details.failure());
                return;
            }
            if ("InProgress".equalsIgnoreCase(details.overallStatus())) {
                return; // still validating - look again on the next run
            }
            details.receipts().forEach(r -> outcomes.put(r.uuid(), r));
            totalPages = Math.max(1, details.totalPages());
        }
        tx.executeWithoutResult(s -> apply(submissionUuid, outcomes));
    }

    void apply(String submissionUuid, Map<String, EtaApiClient.ReceiptOutcome> outcomes) {
        int accepted = 0;
        int rejected = 0;
        for (EInvoiceSubmission row : submissionRepository.findSubmittedBySubmissionUuid(submissionUuid)) {
            EtaApiClient.ReceiptOutcome outcome = outcomes.get(row.getEtaUuid());
            if (outcome == null || outcome.status() == null) {
                continue;
            }
            switch (outcome.status().toLowerCase()) {
                case "valid" -> {
                    row.recordError(EInvoiceSubmission.Status.ACCEPTED, null);
                    accepted++;
                }
                case "invalid", "cancelled" -> {
                    String reason = outcome.errors() != null ? outcome.errors() : "ETA marked the receipt " + outcome.status();
                    row.recordError(EInvoiceSubmission.Status.REJECTED, reason);
                    rejected++;
                }
                default -> {
                    // unknown status - leave it SUBMITTED and look again later
                }
            }
        }
        log.info("ETA submission {} validated | accepted {} | rejected {}", submissionUuid, accepted, rejected);
    }
}
