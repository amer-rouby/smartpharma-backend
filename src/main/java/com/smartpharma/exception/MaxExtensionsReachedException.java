package com.smartpharma.exception;

public class MaxExtensionsReachedException extends RuntimeException {
    public MaxExtensionsReachedException(String message) {
        super(message);
    }
}