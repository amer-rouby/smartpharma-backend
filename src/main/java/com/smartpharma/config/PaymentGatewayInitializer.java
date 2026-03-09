package com.smartpharma.config;

import com.smartpharma.service.FawryPaymentService;
import com.smartpharma.service.InstaPayPaymentService;
import com.smartpharma.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayInitializer implements ApplicationRunner {

    private final PaymentService paymentService;
    private final InstaPayPaymentService instaPayService;
    private final FawryPaymentService fawryService;

    @Override
    public void run(ApplicationArguments args) {
        paymentService.registerGateway(instaPayService);
        paymentService.registerGateway(fawryService);
        log.info("✅ Payment gateways registered: INSTAPAY, FAWRY");
    }
}