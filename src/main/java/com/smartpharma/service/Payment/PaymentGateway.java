package com.smartpharma.service.Payment;

import com.smartpharma.dto.request.PaymentRequest;
import com.smartpharma.dto.response.PaymentResponse;
import com.smartpharma.entity.enums.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse refundPayment(String paymentReference, BigDecimal amount, String reason);

    PaymentResponse cancelPayment(String paymentReference);

    PaymentResponse verifyPayment(String paymentReference);

    PaymentMethod getPaymentMethod();

    boolean isSupported(PaymentMethod method);
}