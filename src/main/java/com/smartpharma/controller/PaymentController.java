package com.smartpharma.controller;

import com.smartpharma.dto.request.PaymentRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.PaymentResponse;
import com.smartpharma.entity.Payment;
import com.smartpharma.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Payment request received: {}", request);
        try {
            PaymentResponse response = paymentService.processPayment(request);
            if ("COMPLETED".equals(response.getStatus())) {
                return ResponseEntity.ok(ApiResponse.success(response, "Payment processed successfully"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(response.getMessage()));
            }
        } catch (Exception e) {
            log.error("Payment processing failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Payment failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{reference}/refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(@PathVariable String reference, @RequestParam BigDecimal amount, @RequestParam(required = false, defaultValue = "Customer request") String reason) {
        log.info("Refund request for: {}, amount: {}", reference, amount);
        try {
            PaymentResponse response = paymentService.refundPayment(reference, amount, reason);
            return ResponseEntity.ok(ApiResponse.success(response, "Refund processed successfully"));
        } catch (Exception e) {
            log.error("Refund failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Refund failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{reference}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(@PathVariable String reference) {
        log.info("Cancel request for: {}", reference);
        try {
            PaymentResponse response = paymentService.cancelPayment(reference);
            return ResponseEntity.ok(ApiResponse.success(response, "Payment cancelled successfully"));
        } catch (Exception e) {
            log.error("Cancellation failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Cancellation failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{reference}/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(@PathVariable String reference) {
        try {
            PaymentResponse response = paymentService.verifyPayment(reference);
            if ("NOT_FOUND".equals(response.getStatus())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(response, "Payment verified"));
        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Verification failed: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<Payment>>> getPayments(@RequestParam Long pharmacyId, Pageable pageable) {
        try {
            Page<Payment> payments = paymentService.getPaymentsByPharmacy(pharmacyId, pageable);
            return ResponseEntity.ok(ApiResponse.success(payments, "Payments retrieved successfully"));
        } catch (Exception e) {
            log.error("Get payments failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to retrieve payments: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentStats(@RequestParam Long pharmacyId) {
        try {
            Map<String, Object> stats = paymentService.getPaymentStats(pharmacyId);
            return ResponseEntity.ok(ApiResponse.success(stats, "Stats retrieved successfully"));
        } catch (Exception e) {
            log.error("Get stats failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to retrieve stats: " + e.getMessage()));
        }
    }
}