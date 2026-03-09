package com.smartpharma.service;

import com.smartpharma.dto.request.PaymentRequest;
import com.smartpharma.dto.response.PaymentResponse;
import com.smartpharma.entity.Payment;
import com.smartpharma.entity.PaymentMethod;
import com.smartpharma.repository.PaymentRepository;
import com.smartpharma.repository.PharmacyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class FawryPaymentService extends BasePaymentGateway {

    @Value("${payment.fawry.merchantCode:}")
    private String merchantCode;

    @Value("${payment.fawry.secretKey:}")
    private String secretKey;

    @Value("${payment.fawry.baseUrl:https://atfawry.fawrystaging.com/}")
    private String baseUrl;

    public FawryPaymentService(PaymentRepository paymentRepository, PharmacyRepository pharmacyRepository) {
        super(paymentRepository, pharmacyRepository);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.FAWRY;
    }

    @Override
    public boolean isSupported(PaymentMethod method) {
        return method == PaymentMethod.FAWRY;
    }

    @Override
    protected PaymentResponse callGatewayAPI(PaymentRequest request, String referenceNumber) {
        log.info("Calling Fawry API for payment: {}", referenceNumber);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("merchantCode", merchantCode);
            payload.put("merchantRefNumber", referenceNumber);
            payload.put("amount", request.getAmount());
            payload.put("customerMobile", request.getCustomerPhone());
            payload.put("customerEmail", request.getCustomerEmail());
            payload.put("description", request.getDescription());

            String signature = generateSignature(referenceNumber, request.getAmount());
            payload.put("signature", signature);

            log.info("Fawry request payload: {}", payload);

            return PaymentResponse.builder()
                    .referenceNumber(referenceNumber)
                    .paymentMethod("FAWRY")
                    .amount(request.getAmount())
                    .status("COMPLETED")
                    .message("Payment processed successfully via Fawry")
                    .build();

        } catch (Exception e) {
            log.error("Fawry API call failed: {}", e.getMessage(), e);
            return PaymentResponse.builder()
                    .referenceNumber(referenceNumber)
                    .status("FAILED")
                    .message("Fawry payment failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    protected PaymentResponse processGatewayRefund(Payment payment, BigDecimal amount, String reason) {
        log.info("Processing Fawry refund for: {}", payment.getReferenceNumber());

        try {
            Map<String, Object> refundPayload = new HashMap<>();
            refundPayload.put("merchantCode", merchantCode);
            refundPayload.put("merchantRefNumber", payment.getReferenceNumber());
            refundPayload.put("amount", amount);
            refundPayload.put("reason", reason);

            String signature = generateSignature(payment.getReferenceNumber(), amount);
            refundPayload.put("signature", signature);

            log.info("Fawry refund request: {}", refundPayload);

            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .referenceNumber(payment.getReferenceNumber())
                    .status("COMPLETED")
                    .message("Refund processed successfully via Fawry")
                    .build();

        } catch (Exception e) {
            log.error("Fawry refund failed: {}", e.getMessage(), e);
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Fawry refund failed: " + e.getMessage())
                    .build();
        }
    }

    private String generateSignature(String referenceNumber, BigDecimal amount) {
        String data = merchantCode + referenceNumber + amount + secretKey;
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(data);
    }
}