package com.smartpharma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "whatsapp.api")
@Data
public class WhatsAppConfig {
    private String baseUrl = "https://graph.facebook.com/v18.0";
    private String phoneNumberId;
    private String accessToken;
}