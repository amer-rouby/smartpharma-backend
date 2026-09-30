package com.smartpharma.einvoice.service.impl;

import com.smartpharma.common.exception.LocalizedException;
import com.smartpharma.einvoice.dto.response.EInvoiceSubmissionResponse;
import com.smartpharma.einvoice.entity.EInvoiceSubmission;
import com.smartpharma.einvoice.repository.EInvoiceSubmissionRepository;
import com.smartpharma.einvoice.service.EInvoiceService;
import com.smartpharma.sales.repository.SaleTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Manual actions on a sale's ETA receipt. Deliberately not @Transactional:
// issuing runs in its own transaction and delivery must not hold one open
// while waiting on ETA.
@Service
@RequiredArgsConstructor
public class EInvoiceServiceImpl implements EInvoiceService {

    private final EInvoiceSubmissionRepository eInvoiceSubmissionRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final EtaReceiptIssuer issuer;
    private final EtaReceiptSubmitter submitter;

    // Issues the receipt if it wasn't issued yet, then sends whatever is pending.
    @Override
    public EInvoiceSubmissionResponse submit(Long saleId, Long pharmacyId) {
        checkEnabled(pharmacyId);
        EInvoiceSubmission submission = issuer.issue(saleId, pharmacyId, false);
        return deliverAndReload(submission, false);
    }

    // Also re-issues a rejected receipt (as a new receipt referencing the old
    // UUID) and resets the attempt counter so a stopped receipt is sent again.
    @Override
    public EInvoiceSubmissionResponse retry(Long saleId, Long pharmacyId) {
        checkEnabled(pharmacyId);
        findSubmission(saleId, pharmacyId);
        EInvoiceSubmission submission = issuer.issue(saleId, pharmacyId, true);
        return deliverAndReload(submission, true);
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

    @Override
    @Transactional(readOnly = true)
    public List<EInvoiceSubmissionResponse> getReturnsForSale(Long saleId, Long pharmacyId) {
        checkEnabled(pharmacyId);
        findSale(saleId, pharmacyId);
        return eInvoiceSubmissionRepository.findReturnsBySaleTransactionId(saleId).stream()
                .map(EInvoiceSubmissionResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EInvoiceSubmissionResponse> getNeedingAttention(Long pharmacyId) {
        checkEnabled(pharmacyId);
        List<Object[]> rows = eInvoiceSubmissionRepository.findNeedingAttention(pharmacyId);
        Map<Long, EInvoiceSubmission> byId = eInvoiceSubmissionRepository
                .findAllById(rows.stream().map(row -> ((Number) row[0]).longValue()).toList())
                .stream()
                .collect(Collectors.toMap(EInvoiceSubmission::getId, Function.identity()));
        return rows.stream()
                .filter(row -> byId.containsKey(((Number) row[0]).longValue()))
                .map(row -> {
                    EInvoiceSubmissionResponse response =
                            EInvoiceSubmissionResponse.fromEntity(byId.get(((Number) row[0]).longValue()));
                    response.setInvoiceNumber((String) row[1]);
                    response.setSaleCancelled(Boolean.TRUE.equals(row[2]));
                    return response;
                })
                .toList();
    }

    // Works on returns of cancelled sales too, which no sale screen shows.
    @Override
    public EInvoiceSubmissionResponse retrySubmission(Long submissionId, Long pharmacyId) {
        checkEnabled(pharmacyId);
        Long owner = eInvoiceSubmissionRepository.findPharmacyIdOf(submissionId).orElse(null);
        EInvoiceSubmission row = pharmacyId.equals(owner)
                ? eInvoiceSubmissionRepository.findById(submissionId).orElse(null)
                : null;
        if (row == null) {
            throw new LocalizedException(HttpStatus.NOT_FOUND, "EINVOICE_SUBMISSION_NOT_FOUND",
                    "E-receipt not found: " + submissionId);
        }
        Long saleId = row.getSaleTransaction().getId();
        EInvoiceSubmission retried;
        if (row.getDocumentType() != EInvoiceSubmission.DocumentType.RETURN) {
            retried = issuer.issue(saleId, pharmacyId, true);
        } else if (row.getSaleReturn() != null) {
            retried = issuer.issuePartialReturn(row.getSaleReturn().getId(), pharmacyId, true).orElse(row);
        } else {
            retried = issuer.issueReturn(saleId, pharmacyId, true).orElse(row);
        }
        return deliverAndReload(retried, true);
    }

    private EInvoiceSubmissionResponse deliverAndReload(EInvoiceSubmission submission, boolean resetAttempts) {
        if (submission.isIssued() && submission.getPosDevice() != null) {
            if (resetAttempts && submission.getStatus() == EInvoiceSubmission.Status.ERROR) {
                EInvoiceSubmission row = eInvoiceSubmissionRepository.findById(submission.getId()).orElseThrow();
                row.setRetryCount(0);
                eInvoiceSubmissionRepository.save(row);
            }
            submitter.deliverDevice(submission.getPosDevice().getId());
        }
        return EInvoiceSubmissionResponse.fromEntity(
                eInvoiceSubmissionRepository.findById(submission.getId()).orElse(submission));
    }

    private void findSale(Long saleId, Long pharmacyId) {
        saleTransactionRepository.findByIdAndPharmacyId(saleId, pharmacyId)
                .orElseThrow(() -> new LocalizedException(HttpStatus.NOT_FOUND, "SALE_NOT_FOUND", "Sale not found"));
    }

    private void findSubmission(Long saleId, Long pharmacyId) {
        findSale(saleId, pharmacyId);
        eInvoiceSubmissionRepository.findBySaleTransactionId(saleId)
                .orElseThrow(() -> new LocalizedException(HttpStatus.NOT_FOUND, "EINVOICE_SUBMISSION_NOT_FOUND",
                        "No e-receipt exists yet for this sale"));
    }

    private void checkEnabled(Long pharmacyId) {
        if (!issuer.isEnabled(pharmacyId)) {
            throw new LocalizedException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_EINVOICE",
                    "E-invoice (ETA) feature is disabled for this pharmacy");
        }
    }
}
