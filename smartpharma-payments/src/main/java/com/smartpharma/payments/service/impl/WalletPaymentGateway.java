package com.smartpharma.payments.service.impl;

import com.smartpharma.payments.dto.request.PaymentRequest;
import com.smartpharma.payments.dto.response.PaymentResponse;
import com.smartpharma.payments.entity.Payment;
import com.smartpharma.payments.entity.enums.PaymentMethod;
import com.smartpharma.payments.repository.PaymentRepository;
import com.smartpharma.common.repository.PharmacyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WalletPaymentGateway extends BasePaymentGateway {

    public WalletPaymentGateway(PaymentRepository paymentRepository, PharmacyRepository pharmacyRepository) {
        super(paymentRepository, pharmacyRepository);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.WALLET;
    }

    @Override
    public boolean isSupported(PaymentMethod method) {
        return false;
    }

    @Override
    protected PaymentResponse callGatewayAPI(PaymentRequest request, String referenceNumber) {
        log.info("Processing Wallet payment: {}", referenceNumber);

        try {
            Thread.sleep(500);

            return PaymentResponse.builder()
                    .status("COMPLETED")
                    .message("Payment processed successfully via Wallet")
                    .referenceNumber(referenceNumber)
                    .paymentMethod(PaymentMethod.WALLET.name())
                    .amount(request.getAmount())
                    .transactionId("WALLET-" + System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("Wallet payment failed: {}", e.getMessage());
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Wallet payment failed: " + e.getMessage())
                    .referenceNumber(referenceNumber)
                    .paymentMethod(PaymentMethod.WALLET.name())
                    .amount(request.getAmount())
                    .build();
        }
    }

    @Override
    protected PaymentResponse processGatewayRefund(Payment payment, java.math.BigDecimal amount, String reason) {
        log.info("Processing Wallet refund: {}, amount: {}", payment.getReferenceNumber(), amount);

        return PaymentResponse.builder()
                .status("COMPLETED")
                .message("Wallet refund processed successfully")
                .referenceNumber(payment.getReferenceNumber())
                .paymentMethod(PaymentMethod.WALLET.name())
                .amount(amount)
                .transactionId("REFUND-WALLET-" + System.currentTimeMillis())
                .build();
    }
}