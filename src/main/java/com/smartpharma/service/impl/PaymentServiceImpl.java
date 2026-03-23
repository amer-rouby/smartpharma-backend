package com.smartpharma.service.impl;

import com.smartpharma.dto.request.PaymentRequest;
import com.smartpharma.dto.response.PaymentResponse;
import com.smartpharma.entity.Payment;
import com.smartpharma.entity.enums.PaymentMethod;
import com.smartpharma.entity.enums.PaymentStatus;
import com.smartpharma.repository.PaymentRepository;
import com.smartpharma.service.Payment.PaymentGateway;
import com.smartpharma.service.Payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final Map<PaymentMethod, PaymentGateway> paymentGateways = new HashMap<>();

    @Override
    public void registerGateway(PaymentGateway gateway) {
        if (gateway != null && gateway.getPaymentMethod() != null) {
            paymentGateways.put(gateway.getPaymentMethod(), gateway);
            log.info("Registered payment gateway: {} for method: {}",
                    gateway.getClass().getSimpleName(),
                    gateway.getPaymentMethod());
        }
    }

    @Override
    public Map<String, String> getRegisteredGateways() {
        Map<String, String> gateways = new HashMap<>();
        for (Map.Entry<PaymentMethod, PaymentGateway> entry : paymentGateways.entrySet()) {
            gateways.put(
                    entry.getKey().name(),
                    entry.getValue().getClass().getSimpleName()
            );
        }
        return gateways;
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment request: {}", request);

        try {
            PaymentMethod method;
            try {
                method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
            } catch (IllegalArgumentException e) {
                return PaymentResponse.builder()
                        .status("FAILED")
                        .message("Unsupported payment method: " + request.getPaymentMethod())
                        .build();
            }

            PaymentGateway gateway = paymentGateways.get(method);
            if (gateway == null) {
                log.error("No gateway found for payment method: {}", method);
                return PaymentResponse.builder()
                        .status("FAILED")
                        .message("Payment gateway not available for: " + method)
                        .build();
            }

            return gateway.processPayment(request);

        } catch (Exception e) {
            log.error("Payment processing failed: {}", e.getMessage(), e);
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Payment failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(String reference, BigDecimal amount, String reason) {
        log.info("Processing refund for: {}, amount: {}", reference, amount);

        return paymentRepository.findByReferenceNumber(reference)
                .map(payment -> {
                    PaymentGateway gateway = paymentGateways.get(payment.getPaymentMethod());
                    if (gateway != null) {
                        return gateway.refundPayment(reference, amount, reason);
                    }
                    return PaymentResponse.builder()
                            .status("FAILED")
                            .message("No gateway found for refund")
                            .build();
                })
                .orElse(PaymentResponse.builder()
                        .status("NOT_FOUND")
                        .message("Payment not found with reference: " + reference)
                        .build());
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(String reference) {
        log.info("Cancelling payment: {}", reference);

        return paymentRepository.findByReferenceNumber(reference)
                .map(payment -> {
                    PaymentGateway gateway = paymentGateways.get(payment.getPaymentMethod());
                    if (gateway != null) {
                        return gateway.cancelPayment(reference);
                    }
                    return PaymentResponse.builder()
                            .status("FAILED")
                            .message("No gateway found for cancellation")
                            .build();
                })
                .orElse(PaymentResponse.builder()
                        .status("NOT_FOUND")
                        .message("Payment not found with reference: " + reference)
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String reference) {
        log.info("Fetching payment by reference: {}", reference);

        return paymentRepository.findByReferenceNumber(reference)
                .map(this::mapToResponse)
                .orElse(PaymentResponse.builder()
                        .status("NOT_FOUND")
                        .message("Payment not found with reference: " + reference)
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByPharmacy(Long pharmacyId, Pageable pageable) {
        return paymentRepository.findByPharmacyId(pharmacyId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStats(Long pharmacyId) {
        Long totalPayments = paymentRepository.countByPharmacyIdAndStatus(
                pharmacyId, PaymentStatus.COMPLETED);
        BigDecimal totalAmount = paymentRepository.getTotalCompletedPayments(pharmacyId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPayments", totalPayments != null ? totalPayments : 0);
        stats.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
        return stats;
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .referenceNumber(payment.getReferenceNumber())
                .paymentMethod(payment.getPaymentMethod() != null
                        ? payment.getPaymentMethod().name() : null)
                .amount(payment.getAmount())
                .status(payment.getStatus() != null
                        ? payment.getStatus().name() : null)
                .message("Payment verified")
                .transactionId(payment.getGatewayTransactionId())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}