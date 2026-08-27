package com.smartpharma.controller;

import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.EInvoiceSubmissionResponse;
import com.smartpharma.service.EInvoiceService;
import com.smartpharma.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// No try/catch - a disabled feature flag or missing sale/submission throws
// LocalizedException, handled globally with the correct status/error code.
@RestController
@RequestMapping("/api/e-invoice")
@RequiredArgsConstructor
@Slf4j
public class EInvoiceController {

    private final EInvoiceService eInvoiceService;

    @GetMapping("/{saleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EInvoiceSubmissionResponse>> getForSale(@PathVariable Long saleId) {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        EInvoiceSubmissionResponse response = eInvoiceService.getForSale(saleId, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{saleId}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EInvoiceSubmissionResponse>> submit(@PathVariable Long saleId) {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        EInvoiceSubmissionResponse response = eInvoiceService.submit(saleId, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{saleId}/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EInvoiceSubmissionResponse>> retry(@PathVariable Long saleId) {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        EInvoiceSubmissionResponse response = eInvoiceService.retry(saleId, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
