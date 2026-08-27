package com.smartpharma.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class MaxExtensionsReachedException extends LocalizedException {
    public MaxExtensionsReachedException(String message) {
        super(HttpStatus.BAD_REQUEST, "MAX_EXTENSIONS_REACHED", message, Map.of());
    }
}
