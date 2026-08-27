package com.smartpharma.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ResourceNotFoundException extends LocalizedException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String errorCode, String message) {
        super(HttpStatus.NOT_FOUND, errorCode, message, Map.of());
    }

    public ResourceNotFoundException(String errorCode, String resourceName, String fieldName, Object fieldValue) {
        super(HttpStatus.NOT_FOUND, errorCode,
                String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue),
                Map.of("resourceName", resourceName, "fieldName", fieldName, "fieldValue", String.valueOf(fieldValue)));
    }
}
