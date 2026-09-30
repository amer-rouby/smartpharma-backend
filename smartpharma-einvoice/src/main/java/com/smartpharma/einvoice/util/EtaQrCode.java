package com.smartpharma.einvoice.util;

import com.smartpharma.einvoice.entity.enums.EtaEnvironment;
// QR content printed on the receipt, per the "Receipt QR Code" section of
// https://sdk.invoicing.eta.gov.eg/receiptissuancefaq/ :
// {portal}/receipts/search/{UUID}/share/{dateTimeIssued UTC}#Total:{total},IssuerRIN:{rin}
public final class EtaQrCode {

    private EtaQrCode() {
    }

    public static String content(EtaEnvironment environment, String uuid, String dateTimeIssued,
                                 String totalAmount, String issuerRin) {
        return environment.portalUrl() + "/receipts/search/" + uuid + "/share/" + dateTimeIssued
                + "#Total:" + totalAmount + ",IssuerRIN:" + issuerRin;
    }
}
