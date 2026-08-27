package com.smartpharma.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // LocalizedException (and everything that extends it - ResourceNotFoundException,
    // SessionExpiredException, AccountLockedException, MaxExtensionsReachedException)
    // carries its own HTTP status + a stable error `code` + interpolation `params`, so
    // this single handler replaces what used to be 4 separate handlers producing 3
    // different JSON shapes. The frontend resolves `code` to ERRORS.<code> in
    // ar.json/en.json instead of showing `message` (English, kept only for server logs
    // and as a fallback for any not-yet-migrated caller).
    @ExceptionHandler(LocalizedException.class)
    public ResponseEntity<ErrorResponse> handleLocalizedException(LocalizedException ex) {
        log.warn("{} [{}]: {}", ex.getStatus(), ex.getErrorCode(), ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                ex.getStatus().value(),
                ex.getMessage(),
                LocalDateTime.now(),
                ex.getErrorCode(),
                ex.getParams()
        );
        return new ResponseEntity<>(error, ex.getStatus());
    }

    // Case-insensitive fragments of a RuntimeException's message that indicate what
    // HTTP status it actually corresponds to. Most services throw a plain
    // `new RuntimeException("X not found")`/`"X already exists"` for business errors
    // instead of a typed exception, so this maps the (already client-safe, hand-written)
    // message to a status code without having to touch every one of those call sites.
    private static final List<String> NOT_FOUND_HINTS = List.of("not found", "no such", "does not exist");
    private static final List<String> CONFLICT_HINTS = List.of(
            "already exists", "already registered", "already in use", "duplicate");
    private static final List<String> FORBIDDEN_HINTS = List.of("access denied", "not authorized", "permission");

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();
        HttpStatus status = inferStatus(message);
        log.error("{}: {}", status, message);
        ErrorResponse error = new ErrorResponse(status.value(), message, LocalDateTime.now());
        return new ResponseEntity<>(error, status);
    }

    private HttpStatus inferStatus(String message) {
        if (message == null) {
            return HttpStatus.BAD_REQUEST;
        }
        String lower = message.toLowerCase();
        if (NOT_FOUND_HINTS.stream().anyMatch(lower::contains)) {
            return HttpStatus.NOT_FOUND;
        }
        if (CONFLICT_HINTS.stream().anyMatch(lower::contains)) {
            return HttpStatus.CONFLICT;
        }
        if (FORBIDDEN_HINTS.stream().anyMatch(lower::contains)) {
            return HttpStatus.FORBIDDEN;
        }
        return HttpStatus.BAD_REQUEST;
    }

    /**
     * Constraint violations (e.g. a race on a unique username/barcode/license number)
     * carry raw driver/SQL text in their message - never return that to the client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "This operation conflicts with existing data (e.g. a value that must be unique is already in use).",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * A NullPointerException is always a bug, never a business rule - its message
     * ("Cannot invoke X because Y is null") is meaningless and potentially revealing
     * to an end user, so it gets the same generic treatment as the Exception fallback.
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException ex) {
        log.error("Unexpected null pointer", ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access Denied: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Access denied",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        log.error("Bad Credentials: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid credentials",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Missing required parameter: " + ex.getParameterName(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        // Same "access denied" treatment as a wrong key, not a generic 400 - a missing
        // X-Platform-Admin-Key should look identical to a wrong one to the caller.
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Access denied",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Generic Exception: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public record ErrorResponse(int status, String message, LocalDateTime timestamp, String code, Map<String, Object> params) {
        public ErrorResponse(int status, String message, LocalDateTime timestamp) {
            this(status, message, timestamp, null, Map.of());
        }
    }
}
