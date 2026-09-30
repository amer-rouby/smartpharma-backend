package com.smartpharma.einvoice.service.impl;

import com.smartpharma.einvoice.entity.EInvoiceSubmission;
import com.smartpharma.einvoice.entity.EtaPosDevice;
import com.smartpharma.einvoice.entity.EtaTaxpayerProfile;
import com.smartpharma.einvoice.repository.EInvoiceSubmissionRepository;
import com.smartpharma.einvoice.repository.EtaPosDeviceRepository;
import com.smartpharma.einvoice.repository.EtaTaxpayerProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

// Sends issued receipts to ETA in batches, per device and in chain order.
// Runs right after each sale (async) and on a timer, so receipts issued while
// ETA or the network was down still go out inside ETA's 24-hour window.
//
// No DB transaction is held while waiting on ETA: rows are read, the HTTP call
// is made, then results are written in a short transaction of their own.
@Service
@Slf4j
public class EtaReceiptSubmitter {

    // ETA limits: 500 receipts / 1.5 MB per submission.
    static final int MAX_BATCH_RECEIPTS = 500;
    static final int MAX_BATCH_BYTES = 1_500_000;
    private static final Set<EInvoiceSubmission.Status> DELIVERABLE =
            EnumSet.of(EInvoiceSubmission.Status.PENDING, EInvoiceSubmission.Status.ERROR);

    private final EInvoiceSubmissionRepository submissionRepository;
    private final EtaPosDeviceRepository deviceRepository;
    private final EtaTaxpayerProfileRepository profileRepository;
    private final EtaSecretCipher cipher;
    private final EtaApiClient apiClient;
    private final TransactionTemplate tx;
    private final int maxAttempts;

    // One delivery at a time per device, so the timer and a manual "submit"
    // can't send the same receipts twice.
    private final Map<Long, ReentrantLock> deviceLocks = new ConcurrentHashMap<>();

    public EtaReceiptSubmitter(EInvoiceSubmissionRepository submissionRepository,
                               EtaPosDeviceRepository deviceRepository,
                               EtaTaxpayerProfileRepository profileRepository,
                               EtaSecretCipher cipher,
                               EtaApiClient apiClient,
                               TransactionTemplate tx,
                               @Value("${eta.submission.max-attempts:50}") int maxAttempts) {
        this.submissionRepository = submissionRepository;
        this.deviceRepository = deviceRepository;
        this.profileRepository = profileRepository;
        this.cipher = cipher;
        this.apiClient = apiClient;
        this.tx = tx;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(initialDelayString = "${eta.submission.initial-delay-ms:60000}",
            fixedDelayString = "${eta.submission.interval-ms:120000}")
    public void deliverAll() {
        if (!cipher.isConfigured()) {
            return;
        }
        List<Long> deviceIds = tx.execute(s -> submissionRepository.findDeviceIdsWithDeliverable(DELIVERABLE, maxAttempts));
        for (Long deviceId : deviceIds == null ? List.<Long>of() : deviceIds) {
            try {
                deliverDevice(deviceId);
            } catch (RuntimeException e) {
                log.error("ETA delivery failed for device {}: {}", deviceId, e.getMessage(), e);
            }
        }
    }

    @Async
    public void deliverDeviceAsync(Long deviceId) {
        try {
            deliverDevice(deviceId);
        } catch (RuntimeException e) {
            log.error("ETA delivery failed for device {}: {}", deviceId, e.getMessage(), e);
        }
    }

    public void deliverDevice(Long deviceId) {
        ReentrantLock lock = deviceLocks.computeIfAbsent(deviceId, id -> new ReentrantLock());
        lock.lock();
        try {
            Delivery delivery = tx.execute(s -> loadDelivery(deviceId));
            if (delivery == null) {
                return;
            }
            for (List<Pending> batch : batches(delivery.pending())) {
                EtaApiClient.SubmitResult result = apiClient.submitBatch(delivery.credentials(), batchJson(batch));
                tx.executeWithoutResult(s -> applyResult(batch, result));
                if (result.failure() != null) {
                    // Keep chain order: don't send later receipts past a batch that didn't go through.
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    record Pending(Long id, String uuid, String json) {
    }

    private record Delivery(EtaApiClient.PosCredentials credentials, List<Pending> pending) {
    }

    private Delivery loadDelivery(Long deviceId) {
        EtaPosDevice device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            return null;
        }
        List<EInvoiceSubmission> rows = submissionRepository.findDeliverable(deviceId, DELIVERABLE, maxAttempts);
        if (rows.isEmpty()) {
            return null;
        }
        Long pharmacyId = device.getPharmacy().getId();
        EtaTaxpayerProfile profile = profileRepository.findByPharmacyId(pharmacyId).orElse(null);
        String problem = profile == null ? "ETA taxpayer settings are not saved"
                : blank(profile.getClientId()) || profile.getClientSecretEncrypted() == null
                ? "ETA client ID / secret are not set" : null;
        if (problem != null) {
            rows.forEach(row -> row.recordError(EInvoiceSubmission.Status.ERROR, problem));
            return null;
        }
        EtaApiClient.PosCredentials credentials = new EtaApiClient.PosCredentials(
                profile.getEnvironment(), profile.getClientId(), cipher.decrypt(profile.getClientSecretEncrypted()),
                device.getSerialNumber(), device.getOsVersion(), device.getModelFramework(),
                cipher.decrypt(device.getPresharedKeyEncrypted()));
        List<Pending> pending = rows.stream()
                .map(row -> new Pending(row.getId(), row.getEtaUuid(), row.getReceiptJson()))
                .toList();
        return new Delivery(credentials, pending);
    }

    static List<List<Pending>> batches(List<Pending> pending) {
        List<List<Pending>> batches = new ArrayList<>();
        List<Pending> current = new ArrayList<>();
        int bytes = 0;
        for (Pending p : pending) {
            int size = p.json().getBytes(StandardCharsets.UTF_8).length + 1;
            if (!current.isEmpty() && (current.size() >= MAX_BATCH_RECEIPTS || bytes + size > MAX_BATCH_BYTES - 64)) {
                batches.add(current);
                current = new ArrayList<>();
                bytes = 0;
            }
            current.add(p);
            bytes += size;
        }
        if (!current.isEmpty()) {
            batches.add(current);
        }
        return batches;
    }

    // The stored receipt texts are spliced in verbatim - re-serializing them
    // could change number formatting and break their UUIDs. Batch signing
    // isn't validated by ETA yet (per the SDK), so signatures is left empty.
    static String batchJson(List<Pending> batch) {
        return "{\"receipts\":[" + batch.stream().map(Pending::json).collect(Collectors.joining(","))
                + "],\"signatures\":[]}";
    }

    private void applyResult(List<Pending> batch, EtaApiClient.SubmitResult result) {
        Map<Long, EInvoiceSubmission> rows = submissionRepository.findAllById(batch.stream().map(Pending::id).toList())
                .stream().collect(Collectors.toMap(EInvoiceSubmission::getId, Function.identity()));
        LocalDateTime now = LocalDateTime.now();

        if (result.failure() != null) {
            for (EInvoiceSubmission row : rows.values()) {
                row.setRetryCount(result.retryable() ? row.getRetryCount() + 1 : maxAttempts);
                row.recordError(EInvoiceSubmission.Status.ERROR, result.failure());
            }
            log.warn("ETA batch of {} not delivered ({}): {}", batch.size(),
                    result.retryable() ? "will retry" : "not retrying", result.failure());
            return;
        }

        Map<String, EtaApiClient.Accepted> accepted = result.accepted().stream()
                .filter(a -> a.uuid() != null)
                .collect(Collectors.toMap(EtaApiClient.Accepted::uuid, Function.identity(), (a, b) -> a));
        Map<String, EtaApiClient.Rejected> rejected = result.rejected().stream()
                .filter(r -> r.uuid() != null)
                .collect(Collectors.toMap(EtaApiClient.Rejected::uuid, Function.identity(), (a, b) -> a));

        for (Pending p : batch) {
            EInvoiceSubmission row = rows.get(p.id());
            if (row == null) {
                continue;
            }
            row.setSubmittedAt(now);
            row.setSubmissionUuid(result.submissionUuid());
            if (accepted.containsKey(p.uuid())) {
                row.setLongId(accepted.get(p.uuid()).longId());
                row.recordError(EInvoiceSubmission.Status.SUBMITTED, null);
            } else if (rejected.containsKey(p.uuid())) {
                row.recordError(EInvoiceSubmission.Status.REJECTED, rejected.get(p.uuid()).message());
            } else {
                row.setRetryCount(row.getRetryCount() + 1);
                row.recordError(EInvoiceSubmission.Status.ERROR, "ETA response didn't mention this receipt");
            }
        }
        log.info("ETA batch {} | sent {} | accepted {} | rejected {}", result.submissionUuid(), batch.size(),
                accepted.size(), rejected.size());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
