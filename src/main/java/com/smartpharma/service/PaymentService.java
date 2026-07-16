package com.smartpharma.service;

import com.smartpharma.dto.request.PaymentRequest;
import com.smartpharma.dto.response.PaymentResponse;
import com.smartpharma.service.Payment.PaymentGateway;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse refundPayment(String reference, BigDecimal amount, String reason);

    PaymentResponse cancelPayment(String reference);

    PaymentResponse getPaymentByReference(String reference);

    Page<PaymentResponse> getPaymentsByPharmacy(Long pharmacyId, String status, String paymentMethod, String search, Pageable pageable);

    Map<String, Object> getPaymentStats(Long pharmacyId);

    void registerGateway(PaymentGateway gateway);

    Map<String, String> getRegisteredGateways();
}