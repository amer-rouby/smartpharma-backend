package com.smartpharma.service;

import com.smartpharma.dto.request.PaymentRequest;
import com.smartpharma.dto.response.PaymentResponse;
import com.smartpharma.entity.Payment;
import com.smartpharma.entity.PaymentMethod;
import com.smartpharma.entity.PaymentStatus;
import com.smartpharma.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final Map<PaymentMethod, PaymentGateway> paymentGateways = new HashMap<>();

    public void registerGateway(PaymentGateway gateway) {
        paymentGateways.put(gateway.getPaymentMethod(), gateway);
        log.info("Registered payment gateway: {}", gateway.getPaymentMethod());
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment request: {}", request);

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Invalid payment method: " + request.getPaymentMethod())
                    .build();
        }

        PaymentGateway gateway = paymentGateways.get(method);
        if (gateway == null) {
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Payment gateway not available for: " + method)
                    .build();
        }

        return gateway.processPayment(request);
    }

    @Transactional
    public PaymentResponse refundPayment(String paymentReference, BigDecimal amount, String reason) {
        log.info("Processing refund for: {}, amount: {}, reason: {}", paymentReference, amount, reason);

        Payment payment = paymentRepository.findByReferenceNumber(paymentReference)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentReference));

        PaymentGateway gateway = paymentGateways.get(payment.getPaymentMethod());
        if (gateway == null) {
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Payment gateway not available")
                    .build();
        }

        return gateway.refundPayment(paymentReference, amount, reason);
    }

    @Transactional
    public PaymentResponse cancelPayment(String paymentReference) {
        log.info("Cancelling payment: {}", paymentReference);

        Payment payment = paymentRepository.findByReferenceNumber(paymentReference)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentReference));

        PaymentGateway gateway = paymentGateways.get(payment.getPaymentMethod());
        if (gateway == null) {
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Payment gateway not available")
                    .build();
        }

        return gateway.cancelPayment(paymentReference);
    }

    @Transactional(readOnly = true)
    public PaymentResponse verifyPayment(String paymentReference) {
        return paymentRepository.findByReferenceNumber(paymentReference)
                .map(payment -> {
                    PaymentGateway gateway = paymentGateways.get(payment.getPaymentMethod());
                    return gateway != null ? gateway.verifyPayment(paymentReference) : null;
                })
                .orElse(PaymentResponse.builder()
                        .status("NOT_FOUND")
                        .message("Payment not found")
                        .build());
    }

    @Transactional(readOnly = true)
    public Page<Payment> getPaymentsByPharmacy(Long pharmacyId, Pageable pageable) {
        return paymentRepository.findByPharmacyId(pharmacyId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByPharmacyAndStatus(Long pharmacyId, String status) {
        return paymentRepository
                .findByPharmacyIdAndStatus(pharmacyId, PaymentStatus.valueOf(status), Pageable.unpaged())
                .getContent();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStats(Long pharmacyId) {
        Long totalPayments = paymentRepository.countByPharmacyIdAndStatus(pharmacyId, PaymentStatus.COMPLETED);
        BigDecimal totalAmount = paymentRepository.getTotalCompletedPayments(pharmacyId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPayments", totalPayments != null ? totalPayments : 0);
        stats.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
        return stats;
    }

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
}