package com.smartpharma.service.Payment;

import com.smartpharma.dto.request.PaymentRequest;
import com.smartpharma.dto.response.PaymentResponse;
import com.smartpharma.entity.Payment;
import com.smartpharma.entity.enums.PaymentMethod;
import com.smartpharma.entity.enums.PaymentStatus;
import com.smartpharma.repository.PaymentRepository;
import com.smartpharma.repository.PharmacyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class VisaPaymentGateway extends BasePaymentGateway {

    public VisaPaymentGateway(PaymentRepository paymentRepository, PharmacyRepository pharmacyRepository) {
        super(paymentRepository, pharmacyRepository);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.VISA;
    }

    @Override
    public boolean isSupported(PaymentMethod method) {
        return method == PaymentMethod.VISA;
    }

    @Override
    protected PaymentResponse callGatewayAPI(PaymentRequest request, String referenceNumber) {
        log.info("Processing VISA payment: {}", referenceNumber);

        try {
            Thread.sleep(1000);

            return PaymentResponse.builder()
                    .status("COMPLETED")
                    .message("Payment processed successfully via VISA")
                    .referenceNumber(referenceNumber)
                    .paymentMethod(PaymentMethod.VISA.name())
                    .amount(request.getAmount())
                    .transactionId("VISA-" + System.currentTimeMillis())
                    .paidAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("VISA payment failed: {}", e.getMessage());
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("VISA payment failed: " + e.getMessage())
                    .referenceNumber(referenceNumber)
                    .paymentMethod(PaymentMethod.VISA.name())
                    .amount(request.getAmount())
                    .build();
        }
    }

    @Override
    protected PaymentResponse processGatewayRefund(Payment payment, BigDecimal amount, String reason) {
        log.info("Processing VISA refund for: {}, amount: {}", payment.getReferenceNumber(), amount);

        try {
            Thread.sleep(500);

            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            return PaymentResponse.builder()
                    .status("COMPLETED")
                    .message("Refund processed successfully via VISA")
                    .referenceNumber(payment.getReferenceNumber())
                    .paymentMethod(PaymentMethod.VISA.name())
                    .amount(amount)
                    .transactionId("REF-VISA-" + System.currentTimeMillis())
                    .paidAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("VISA refund failed: {}", e.getMessage());
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Refund failed: " + e.getMessage())
                    .referenceNumber(payment.getReferenceNumber())
                    .paymentMethod(PaymentMethod.VISA.name())
                    .amount(amount)
                    .build();
        }
    }
}