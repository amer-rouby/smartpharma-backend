package com.smartpharma.dto.request;

import lombok.*;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShareLinkRequest {

    @NotNull
    private String entityType; // "PREDICTION", "REPORT", etc.

    @NotNull
    private Long entityId;

    @Min(value = 1, message = "Expiry hours must be at least 1")
    private Integer expiryHours = 24; // Default 24 hours
}