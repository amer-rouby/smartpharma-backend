package com.smartpharma.einvoice.service.impl;

import com.smartpharma.einvoice.util.EtaQrCode;
import com.smartpharma.einvoice.exception.EtaReceiptException;
import com.smartpharma.common.exception.LocalizedException;
import com.smartpharma.einvoice.entity.EInvoiceSubmission;
import com.smartpharma.einvoice.entity.EtaPosDevice;
import com.smartpharma.einvoice.entity.EtaTaxpayerProfile;
import com.smartpharma.einvoice.repository.EInvoiceSubmissionRepository;
import com.smartpharma.einvoice.repository.EtaPosDeviceRepository;
import com.smartpharma.einvoice.repository.EtaTaxpayerProfileRepository;
import com.smartpharma.sales.entity.SaleTransaction;
import com.smartpharma.sales.repository.SaleTransactionRepository;
import com.smartpharma.settings.repository.PharmacySettingsRepository;
import com.smartpharma.settings.service.SmartFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

// Turns a sale into an issued ETA receipt: builds it, hashes it onto the
// device's chain and stores the exact JSON. Sending it is EtaReceiptSubmitter's
// job, so a sale is never blocked on ETA being reachable.
@Service
@RequiredArgsConstructor
@Slf4j
public class EtaReceiptIssuer {

    private final SaleTransactionRepository saleTransactionRepository;
    private final EInvoiceSubmissionRepository submissionRepository;
    private final EtaTaxpayerProfileRepository profileRepository;
    private final EtaPosDeviceRepository deviceRepository;
    private final PharmacySettingsRepository pharmacySettingsRepository;
    private final SmartFeatureSettingsService smartFeatureSettingsService;

    public boolean isEnabled(Long pharmacyId) {
        return Boolean.TRUE.equals(smartFeatureSettingsService.getOrCreate(pharmacyId).getEInvoiceEnabled());
    }

    // Called right after a sale commits. Does nothing unless the pharmacy has
    // e-receipts switched on and a taxpayer profile saved - pharmacies that
    // don't use the feature never get submission rows.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<EInvoiceSubmission> issueAfterSale(Long saleId, Long pharmacyId) {
        if (!isEnabled(pharmacyId) || profileRepository.findByPharmacyId(pharmacyId).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(issue(saleId, pharmacyId, false));
    }

    // Issues the receipt for a sale if it hasn't been issued yet. With
    // reissueRejected, a receipt ETA rejected is rebuilt (after the pharmacy
    // fixed its data) as a new receipt pointing back at the old UUID.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EInvoiceSubmission issue(Long saleId, Long pharmacyId, boolean reissueRejected) {
        SaleTransaction sale = saleTransactionRepository.findByIdAndPharmacyId(saleId, pharmacyId)
                .orElseThrow(() -> new LocalizedException(HttpStatus.NOT_FOUND, "SALE_NOT_FOUND", "Sale not found"));

        EInvoiceSubmission submission = submissionRepository.findBySaleTransactionId(saleId)
                .orElseGet(() -> EInvoiceSubmission.builder().saleTransaction(sale).build());

        boolean rejected = submission.getStatus() == EInvoiceSubmission.Status.REJECTED;
        if (submission.isIssued() && !(reissueRejected && rejected)) {
            return submission;
        }
        String referenceOldUuid = rejected ? submission.getEtaUuid() : null;

        EtaTaxpayerProfile profile = profileRepository.findByPharmacyId(pharmacyId).orElse(null);
        if (profile == null) {
            submission.recordError(EInvoiceSubmission.Status.ERROR, "ETA taxpayer settings are not saved yet");
            return submissionRepository.save(submission);
        }

        // Locks the device row until commit - the chain head below is read
        // and advanced atomically with saving this receipt.
        List<EtaPosDevice> devices = deviceRepository.findActiveForUpdate(pharmacyId);
        if (devices.size() != 1) {
            submission.recordError(EInvoiceSubmission.Status.ERROR, devices.isEmpty()
                    ? "No active ETA POS device is registered"
                    : "More than one active ETA POS device - keep exactly one active");
            return submissionRepository.save(submission);
        }
        EtaPosDevice device = devices.get(0);

        String currency = pharmacySettingsRepository.findByPharmacyId(pharmacyId)
                .map(s -> s.getCurrency()).orElse("EGP");
        String previousUuid = device.getLastReceiptUuid() == null ? "" : device.getLastReceiptUuid();

        EtaReceiptBuilder.BuiltReceipt built;
        try {
            // transactionDate is LocalDateTime.now() on the server, so it's in the JVM zone.
            built = EtaReceiptBuilder.build(sale, profile, device.getSerialNumber(), previousUuid,
                    referenceOldUuid, currency, ZoneId.systemDefault());
        } catch (EtaReceiptException e) {
            // Not issued: the chain doesn't move, the sale can be retried once fixed.
            submission.recordError(EInvoiceSubmission.Status.ERROR, "Receipt not issued: " + e.getMessage());
            log.warn("ETA receipt not issued for sale {}: {}", saleId, e.getMessage());
            return submissionRepository.save(submission);
        }

        device.setLastReceiptUuid(built.uuid());
        deviceRepository.save(device);

        submission.setPosDevice(device);
        submission.setEtaUuid(built.uuid());
        submission.setPreviousUuid(previousUuid);
        submission.setReceiptNumber(built.receiptNumber());
        submission.setDateTimeIssued(built.dateTimeIssued());
        submission.setReceiptJson(built.json());
        submission.setQrContent(EtaQrCode.content(profile.getEnvironment(), built.uuid(), built.dateTimeIssued(),
                built.totalAmount(), profile.getRin()));
        submission.setSubmissionUuid(null);
        submission.setLongId(null);
        submission.setSubmittedAt(null);
        submission.setRetryCount(0);
        submission.recordError(EInvoiceSubmission.Status.PENDING, null);
        log.info("ETA receipt issued | sale {} | uuid {} | device {}", saleId, built.uuid(), device.getSerialNumber());
        return submissionRepository.save(submission);
    }
}
