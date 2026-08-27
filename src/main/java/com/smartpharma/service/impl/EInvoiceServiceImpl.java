package com.smartpharma.service.impl;

import com.smartpharma.dto.response.EInvoiceSubmissionResponse;
import com.smartpharma.entity.EInvoiceSubmission;
import com.smartpharma.entity.SaleTransaction;
import com.smartpharma.exception.LocalizedException;
import com.smartpharma.repository.EInvoiceSubmissionRepository;
import com.smartpharma.repository.SaleTransactionRepository;
import com.smartpharma.service.EInvoiceService;
import com.smartpharma.service.EtaIntegrationService;
import com.smartpharma.service.EtaSubmissionResult;
import com.smartpharma.service.settings.SmartFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EInvoiceServiceImpl implements EInvoiceService {

    private final EInvoiceSubmissionRepository eInvoiceSubmissionRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final EtaIntegrationService etaIntegrationService;
    private final SmartFeatureSettingsService smartFeatureSettingsService;

    @Override
    @Transactional
    public EInvoiceSubmissionResponse submit(Long saleId, Long pharmacyId) {
        checkEnabled(pharmacyId);
        SaleTransaction sale = findSale(saleId, pharmacyId);

        EInvoiceSubmission submission = eInvoiceSubmissionRepository.findBySaleTransactionId(saleId)
                .orElseGet(() -> EInvoiceSubmission.builder().saleTransaction(sale).build());

        attemptSubmission(submission);
        return EInvoiceSubmissionResponse.fromEntity(eInvoiceSubmissionRepository.save(submission));
    }

    @Override
    @Transactional
    public EInvoiceSubmissionResponse retry(Long saleId, Long pharmacyId) {
        checkEnabled(pharmacyId);
        findSale(saleId, pharmacyId);

        EInvoiceSubmission submission = eInvoiceSubmissionRepository.findBySaleTransactionId(saleId)
                .orElseThrow(() -> new LocalizedException(HttpStatus.NOT_FOUND, "EINVOICE_SUBMISSION_NOT_FOUND",
                        "No e-invoice submission exists yet for this sale"));

        submission.setRetryCount(submission.getRetryCount() + 1);
        attemptSubmission(submission);
        return EInvoiceSubmissionResponse.fromEntity(eInvoiceSubmissionRepository.save(submission));
    }

    @Override
    @Transactional(readOnly = true)
    public EInvoiceSubmissionResponse getForSale(Long saleId, Long pharmacyId) {
        checkEnabled(pharmacyId);
        findSale(saleId, pharmacyId);
        return eInvoiceSubmissionRepository.findBySaleTransactionId(saleId)
                .map(EInvoiceSubmissionResponse::fromEntity)
                .orElse(null);
    }

    private SaleTransaction findSale(Long saleId, Long pharmacyId) {
        return saleTransactionRepository.findByIdAndPharmacyId(saleId, pharmacyId)
                .orElseThrow(() -> new LocalizedException(HttpStatus.NOT_FOUND, "SALE_NOT_FOUND", "Sale not found"));
    }

    private void attemptSubmission(EInvoiceSubmission submission) {
        EtaSubmissionResult result = etaIntegrationService.submit(submission.getSaleTransaction());
        submission.setSubmittedAt(LocalDateTime.now());
        if (result.success()) {
            submission.setStatus(EInvoiceSubmission.Status.SUBMITTED);
            submission.setEtaUuid(result.etaUuid());
            submission.setErrorMessage(null);
        } else {
            submission.setStatus(EInvoiceSubmission.Status.ERROR);
            submission.setErrorMessage(result.errorMessage());
        }
    }

    private void checkEnabled(Long pharmacyId) {
        Boolean enabled = smartFeatureSettingsService.getOrCreate(pharmacyId).getEInvoiceEnabled();
        if (enabled != null && !enabled) {
            throw new LocalizedException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_EINVOICE",
                    "E-invoice (ETA) feature is disabled for this pharmacy");
        }
    }
}
