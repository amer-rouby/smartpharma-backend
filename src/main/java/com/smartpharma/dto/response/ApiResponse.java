package com.smartpharma.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API response wrapper for consistent response format.
 * @param <T> The type of data in the response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Integer statusCode;

    /**
     * Create a successful response.
     * @param data The response data
     * @param message Success message
     * @param <T> The data type
     * @return ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(200)
                .build();
    }

    /**
     * Create an error response.
     * @param message Error message
     * @param code HTTP status code
     * @param <T> The data type (usually Void for errors)
     * @return ApiResponse with success=false
     */
    public static <T> ApiResponse<T> error(String message, Integer code) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .statusCode(code)
                .build();
    }

    /**
     * Create an error response with default 500 status.
     * @param message Error message
     * @param <T> The data type
     * @return ApiResponse with success=false and statusCode=500
     */
    public static <T> ApiResponse<T> error(String message) {
        return error(message, 500);
    }
}