package com.smartpharma.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

// Base for every exception meant to reach the client as a translated message.
// `message` stays plain English (server logs, and the fallback if a frontend
// build somehow predates a newly-added errorCode) while `errorCode` + `params`
// let the frontend resolve ERRORS.<errorCode> in ar.json/en.json with
// {{param}} interpolation instead of showing this raw string.
@Getter
public class LocalizedException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, Object> params;

    public LocalizedException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, Collections.emptyMap());
    }

    public LocalizedException(HttpStatus status, String errorCode, String message, Map<String, Object> params) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.params = params == null ? Collections.emptyMap() : params;
    }

    // Cause-preserving overloads for call sites that used to do
    // `new RuntimeException(message, cause)` - keeps the original cause chain
    // (and therefore full "Caused by: ..." stack traces in server logs) intact
    // when migrating those sites to LocalizedException.
    public LocalizedException(HttpStatus status, String errorCode, String message, Throwable cause) {
        this(status, errorCode, message, Collections.emptyMap(), cause);
    }

    public LocalizedException(HttpStatus status, String errorCode, String message, Map<String, Object> params, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.params = params == null ? Collections.emptyMap() : params;
    }
}
