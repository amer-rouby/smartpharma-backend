package com.smartpharma.payments.config;

import com.smartpharma.payments.service.impl.PaymentGateway;
import com.smartpharma.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayInitializer implements ApplicationRunner {

    private final PaymentService paymentService;
    private final List<PaymentGateway> paymentGateways;

    @Override
    public void run(ApplicationArguments args) {
        for (PaymentGateway gateway : paymentGateways) {
            paymentService.registerGateway(gateway);
            log.info("✓ Registered gateway: {} for method: {}",
                    gateway.getClass().getSimpleName(),
                    gateway.getPaymentMethod());
        }
        log.info("Total registered gateways: {}", paymentGateways.size());
    }
}