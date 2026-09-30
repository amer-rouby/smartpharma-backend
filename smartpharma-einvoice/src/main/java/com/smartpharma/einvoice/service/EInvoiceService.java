package com.smartpharma.einvoice.service;

import com.smartpharma.einvoice.dto.response.EInvoiceSubmissionResponse;

import java.util.List;

public interface EInvoiceService {
    EInvoiceSubmissionResponse submit(Long saleId, Long pharmacyId);

    EInvoiceSubmissionResponse retry(Long saleId, Long pharmacyId);

    EInvoiceSubmissionResponse getForSale(Long saleId, Long pharmacyId);

    // Return receipts of a sale (cancellation and partial returns).
    List<EInvoiceSubmissionResponse> getReturnsForSale(Long saleId, Long pharmacyId);

    // Rejected or failed receipts, sales and returns, newest first.
    List<EInvoiceSubmissionResponse> getNeedingAttention(Long pharmacyId);

    // Retry (re-issue if rejected) any receipt, sale or return, by its id.
    EInvoiceSubmissionResponse retrySubmission(Long submissionId, Long pharmacyId);
}
