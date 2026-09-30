package com.smartpharma.einvoice.entity.enums;

// ETA base addresses per environment - from the SDK FAQ
// (https://sdk.invoicing.eta.gov.eg/faq/). PREPROD uses certificates issued
// by ETA's own test root CA, which must be trusted by the JVM truststore -
// never disable TLS verification to get around it.
public enum EtaEnvironment {
    PREPROD("https://id.preprod.eta.gov.eg", "https://api.preprod.invoicing.eta.gov.eg",
            "https://preprod.invoicing.eta.gov.eg"),
    PROD("https://id.eta.gov.eg", "https://api.invoicing.eta.gov.eg",
            "https://invoicing.eta.gov.eg");

    private final String identityUrl;
    private final String apiUrl;
    private final String portalUrl;

    EtaEnvironment(String identityUrl, String apiUrl, String portalUrl) {
        this.identityUrl = identityUrl;
        this.apiUrl = apiUrl;
        this.portalUrl = portalUrl;
    }

    public String identityUrl() {
        return identityUrl;
    }

    public String apiUrl() {
        return apiUrl;
    }

    public String portalUrl() {
        return portalUrl;
    }
}
