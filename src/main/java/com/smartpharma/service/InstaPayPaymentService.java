package com.smartpharma.service;

import com.smartpharma.dto.request.PaymentRequest;
import com.smartpharma.dto.response.PaymentResponse;
import com.smartpharma.entity.Payment;
import com.smartpharma.entity.PaymentMethod;
import com.smartpharma.repository.PaymentRepository;
import com.smartpharma.repository.PharmacyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class InstaPayPaymentService extends BasePaymentGateway {

    public InstaPayPaymentService(PaymentRepository paymentRepository, PharmacyRepository pharmacyRepository) {
        super(paymentRepository, pharmacyRepository);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.INSTAPAY;
    }

    @Override
    public boolean isSupported(PaymentMethod method) {
        return method == PaymentMethod.INSTAPAY;
    }

    @Override
    protected PaymentResponse callGatewayAPI(PaymentRequest request, String referenceNumber) {
        log.info("Processing InstaPay payment: {}", referenceNumber);

        try {
            String transactionId = UUID.randomUUID().toString();

            Map<String, Object> payload = new HashMap<>();
            payload.put("transactionId", transactionId);
            payload.put("referenceNumber", referenceNumber);
            payload.put("amount", request.getAmount());
            payload.put("customerPhone", request.getCustomerPhone());
            payload.put("description", request.getDescription());

            log.info("InstaPay request: {}", payload);

            return PaymentResponse.builder()
                    .referenceNumber(referenceNumber)
                    .paymentMethod("INSTAPAY")
                    .amount(request.getAmount())
                    .status("COMPLETED")
                    .message("Payment processed successfully via InstaPay")
                    .build();

        } catch (Exception e) {
            log.error("InstaPay payment failed: {}", e.getMessage(), e);
            return PaymentResponse.builder()
                    .referenceNumber(referenceNumber)
                    .status("FAILED")
                    .message("InstaPay payment failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    protected PaymentResponse processGatewayRefund(Payment payment, BigDecimal amount, String reason) {
        log.info("Processing InstaPay refund for: {}", payment.getReferenceNumber());

        try {
            String refundId = UUID.randomUUID().toString();

            Map<String, Object> refundPayload = new HashMap<>();
            refundPayload.put("refundId", refundId);
            refundPayload.put("originalReference", payment.getReferenceNumber());
            refundPayload.put("amount", amount);
            refundPayload.put("reason", reason);

            log.info("InstaPay refund request: {}", refundPayload);

            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .referenceNumber(payment.getReferenceNumber())
                    .status("COMPLETED")
                    .message("Refund processed successfully via InstaPay")
                    .build();

        } catch (Exception e) {
            log.error("InstaPay refund failed: {}", e.getMessage(), e);
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("InstaPay refund failed: " + e.getMessage())
                    .build();
        }
    }
}