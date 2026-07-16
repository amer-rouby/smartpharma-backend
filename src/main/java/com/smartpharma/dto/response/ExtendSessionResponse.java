package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtendSessionResponse {
    private boolean success;
    private String expiresAt;
    private int remainingExtensions;
    private String message;
}