package com.smartpharma.einvoice.exception;

// A sale that can't be turned into a valid ETA receipt (missing taxpayer
// data, uncoded product, unsupported case). The message is shown to the
// pharmacy as-is, so it says what to fix.
public class EtaReceiptException extends RuntimeException {

    public EtaReceiptException(String message) {
        super(message);
    }
}
