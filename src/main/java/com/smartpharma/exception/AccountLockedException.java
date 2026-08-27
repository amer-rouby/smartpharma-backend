package com.smartpharma.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class AccountLockedException extends LocalizedException {

    public AccountLockedException(String message, Map<String, Object> params) {
        super(HttpStatus.LOCKED, "ACCOUNT_LOCKED", message, params);
    }
}
