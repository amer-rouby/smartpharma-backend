package com.smartpharma.service;

import com.smartpharma.dto.response.EInvoiceSubmissionResponse;

public interface EInvoiceService {
    EInvoiceSubmissionResponse submit(Long saleId, Long pharmacyId);

    EInvoiceSubmissionResponse retry(Long saleId, Long pharmacyId);

    EInvoiceSubmissionResponse getForSale(Long saleId, Long pharmacyId);
}
