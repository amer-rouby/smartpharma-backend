package com.smartpharma.service;

import com.smartpharma.dto.request.PaymentRequest;
import com.smartpharma.dto.response.PaymentResponse;
import com.smartpharma.entity.Payment;
import com.smartpharma.entity.PaymentMethod;
import com.smartpharma.entity.PaymentStatus;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.repository.PaymentRepository;
import com.smartpharma.repository.PharmacyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public abstract class BasePaymentGateway implements PaymentGateway {

    protected final PaymentRepository paymentRepository;
    protected final PharmacyRepository pharmacyRepository;

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment via {}", getPaymentMethod());

        try {
            Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
                    .orElseThrow(() -> new RuntimeException("Pharmacy not found: " + request.getPharmacyId()));

            String referenceNumber = generateReferenceNumber();

            Payment payment = Payment.builder()
                    .pharmacy(pharmacy)
                    .referenceNumber(referenceNumber)
                    .paymentMethod(getPaymentMethod())
                    .amount(request.getAmount())
                    .status(PaymentStatus.PENDING)
                    .customerName(request.getCustomerName())
                    .customerPhone(request.getCustomerPhone())
                    .customerEmail(request.getCustomerEmail())
                    .description(request.getDescription())
                    .createdAt(LocalDateTime.now())
                    .build();

            Payment savedPayment = paymentRepository.save(payment);
            log.info("Payment created with reference: {}", referenceNumber);

            PaymentResponse gatewayResponse = callGatewayAPI(request, referenceNumber);

            updatePaymentStatus(savedPayment, gatewayResponse);

            return gatewayResponse;

        } catch (Exception e) {
            log.error("Payment processing failed: {}", e.getMessage(), e);
            return createErrorResponse(request, e.getMessage());
        }
    }

    @Override
    public PaymentResponse refundPayment(String paymentReference, java.math.BigDecimal amount, String reason) {
        log.info("Processing refund for: {}, amount: {}, reason: {}", paymentReference, amount, reason);

        try {
            Payment payment = paymentRepository.findByReferenceNumber(paymentReference)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentReference));

            if (!payment.isCompleted()) {
                throw new RuntimeException("Cannot refund non-completed payment");
            }

            PaymentResponse gatewayResponse = processGatewayRefund(payment, amount, reason);

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            return gatewayResponse;

        } catch (Exception e) {
            log.error("Refund failed: {}", e.getMessage(), e);
            return createRefundErrorResponse(paymentReference, e.getMessage());
        }
    }

    @Override
    public PaymentResponse cancelPayment(String paymentReference) {
        log.info("Cancelling payment: {}", paymentReference);

        try {
            Payment payment = paymentRepository.findByReferenceNumber(paymentReference)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentReference));

            if (payment.isCompleted()) {
                throw new RuntimeException("Cannot cancel completed payment - use refund instead");
            }

            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .referenceNumber(payment.getReferenceNumber())
                    .status("CANCELLED")
                    .message("Payment cancelled successfully")
                    .build();

        } catch (Exception e) {
            log.error("Cancellation failed: {}", e.getMessage(), e);
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Cancellation failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResponse verifyPayment(String paymentReference) {
        log.info("Verifying payment: {}", paymentReference);

        return paymentRepository.findByReferenceNumber(paymentReference)
                .map(payment -> PaymentResponse.builder()
                        .paymentId(payment.getId())
                        .referenceNumber(payment.getReferenceNumber())
                        .paymentMethod(payment.getPaymentMethod().name())
                        .amount(payment.getAmount())
                        .status(payment.getStatus().name())
                        .paidAt(payment.getPaidAt())
                        .createdAt(payment.getCreatedAt())
                        .build())
                .orElse(PaymentResponse.builder()
                        .status("NOT_FOUND")
                        .message("Payment not found")
                        .build());
    }

    protected abstract PaymentResponse callGatewayAPI(PaymentRequest request, String referenceNumber);
    protected abstract PaymentResponse processGatewayRefund(Payment payment, java.math.BigDecimal amount, String reason);

    protected String generateReferenceNumber() {
        return "PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    protected void updatePaymentStatus(Payment payment, PaymentResponse response) {
        if ("COMPLETED".equals(response.getStatus())) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setGatewayTransactionId(response.getReferenceNumber());
        } else if ("FAILED".equals(response.getStatus())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailedAt(LocalDateTime.now());
            payment.setFailureReason(response.getMessage());
        }
        payment.setGatewayResponse(response.toString());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    protected PaymentResponse createErrorResponse(PaymentRequest request, String message) {
        return PaymentResponse.builder()
                .status("FAILED")
                .message("Payment failed: " + message)
                .build();
    }

    protected PaymentResponse createRefundErrorResponse(String reference, String message) {
        return PaymentResponse.builder()
                .referenceNumber(reference)
                .status("FAILED")
                .message("Refund failed: " + message)
                .build();
    }
}