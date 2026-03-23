package com.smartpharma.service.Payment;

import com.smartpharma.dto.request.PaymentRequest;
import com.smartpharma.dto.response.PaymentResponse;
import com.smartpharma.entity.Payment;
import com.smartpharma.entity.enums.PaymentMethod;
import com.smartpharma.entity.enums.PaymentStatus;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.repository.PaymentRepository;
import com.smartpharma.repository.PharmacyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
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

            PaymentResponse gatewayResponse = callGatewayAPI(request, referenceNumber);

            Payment payment = Payment.builder()
                    .pharmacy(pharmacy)
                    .referenceNumber(referenceNumber)
                    .paymentMethod(getPaymentMethod())
                    .amount(request.getAmount())
                    .status(PaymentStatus.valueOf(gatewayResponse.getStatus()))
                    .gatewayTransactionId(gatewayResponse.getTransactionId())
                    .paidAt(gatewayResponse.getPaidAt())
                    .createdAt(LocalDateTime.now())
                    .customerName(request.getCustomerName())
                    .customerPhone(request.getCustomerPhone())
                    .customerEmail(request.getCustomerEmail())
                    .description(request.getDescription())
                    .build();

            paymentRepository.save(payment);

            return gatewayResponse;

        } catch (Exception e) {
            log.error("Payment processing failed: {}", e.getMessage(), e);
            return createErrorResponse(e.getMessage());
        }
    }

    @Override
    public PaymentResponse refundPayment(String paymentReference, BigDecimal amount, String reason) {
        try {
            Payment payment = paymentRepository.findByReferenceNumber(paymentReference)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentReference));

            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                return createErrorResponse("Payment must be COMPLETED to refund");
            }

            return processGatewayRefund(payment, amount, reason);

        } catch (Exception e) {
            log.error("Refund failed: {}", e.getMessage(), e);
            return createErrorResponse("Refund failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse cancelPayment(String paymentReference) {
        try {
            Payment payment = paymentRepository.findByReferenceNumber(paymentReference)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentReference));

            if (payment.getStatus() != PaymentStatus.PENDING) {
                return createErrorResponse("Only PENDING payments can be cancelled");
            }

            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);

            return PaymentResponse.builder()
                    .status("CANCELLED")
                    .message("Payment cancelled successfully")
                    .referenceNumber(paymentReference)
                    .paymentMethod(getPaymentMethod().name())
                    .build();

        } catch (Exception e) {
            log.error("Cancellation failed: {}", e.getMessage(), e);
            return createErrorResponse("Cancellation failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse verifyPayment(String paymentReference) {
        try {
            Payment payment = paymentRepository.findByReferenceNumber(paymentReference)
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentReference));

            return PaymentResponse.builder()
                    .status(payment.getStatus().name())
                    .message("Payment verified")
                    .referenceNumber(payment.getReferenceNumber())
                    .paymentMethod(payment.getPaymentMethod().name())
                    .amount(payment.getAmount())
                    .transactionId(payment.getGatewayTransactionId())
                    .paidAt(payment.getPaidAt())
                    .build();

        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage(), e);
            return createErrorResponse("Verification failed: " + e.getMessage());
        }
    }

    @Override
    public boolean isSupported(PaymentMethod method) {
        return getPaymentMethod() == method;
    }

    protected abstract PaymentResponse callGatewayAPI(PaymentRequest request, String referenceNumber);

    protected abstract PaymentResponse processGatewayRefund(Payment payment, BigDecimal amount, String reason);

    protected String generateReferenceNumber() {
        return "PAY-" + getPaymentMethod().name().substring(0, 3) + "-" +
                System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    protected PaymentResponse createErrorResponse(String message) {
        return PaymentResponse.builder()
                .status("FAILED")
                .message(message)
                .build();
    }
}