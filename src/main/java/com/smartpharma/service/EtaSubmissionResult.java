package com.smartpharma.service;

// Result of one attempt to submit a sale to the Egyptian Tax Authority (ETA).
public record EtaSubmissionResult(boolean success, String etaUuid, String errorMessage) {

    public static EtaSubmissionResult error(String message) {
        return new EtaSubmissionResult(false, null, message);
    }
}
