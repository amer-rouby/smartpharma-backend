package com.smartpharma.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class SessionExpiredException extends LocalizedException {
    public SessionExpiredException(String message) {
        super(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", message, Map.of());
    }
}
