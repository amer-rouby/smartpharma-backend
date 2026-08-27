package com.smartpharma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Egyptian Tax Authority (ETA) e-invoice credentials - see docs/eta-integration.md.
// None of these have a real default; they're empty until an operator sets
// the ETA_CLIENT_ID/ETA_CLIENT_SECRET/ETA_ENVIRONMENT env vars.
@Configuration
@ConfigurationProperties(prefix = "eta")
@Data
public class EtaConfig {
    private String clientId = "";
    private String clientSecret = "";
    private String environment = "sandbox";

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
