package com.smartpharma.service;

import com.smartpharma.dto.request.PaymentRequest;
import com.smartpharma.dto.response.PaymentResponse;
import com.smartpharma.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGateway {

    PaymentResponse processPayment(PaymentRequest request);
    PaymentResponse refundPayment(String paymentReference, BigDecimal amount, String reason);
    PaymentResponse cancelPayment(String paymentReference);
    PaymentResponse verifyPayment(String paymentReference);
    PaymentMethod getPaymentMethod();
    boolean isSupported(PaymentMethod method);
}