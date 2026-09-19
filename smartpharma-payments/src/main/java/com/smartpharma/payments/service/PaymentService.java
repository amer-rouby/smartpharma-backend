package com.smartpharma.payments.service;

import com.smartpharma.payments.dto.request.PaymentRequest;
import com.smartpharma.payments.dto.response.PaymentResponse;
import com.smartpharma.payments.service.impl.PaymentGateway;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse refundPayment(String reference, Long pharmacyId, BigDecimal amount, String reason);

    PaymentResponse cancelPayment(String reference, Long pharmacyId);

    PaymentResponse getPaymentByReference(String reference, Long pharmacyId);

    Page<PaymentResponse> getPaymentsByPharmacy(Long pharmacyId, String status, String paymentMethod, String search, Pageable pageable);

    Map<String, Object> getPaymentStats(Long pharmacyId);

    void registerGateway(PaymentGateway gateway);

    Map<String, String> getRegisteredGateways();
}