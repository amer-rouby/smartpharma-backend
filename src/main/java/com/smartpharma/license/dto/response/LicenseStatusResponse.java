package com.smartpharma.license.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseStatusResponse {
    private boolean expired;
    // Null when the pharmacy has never been licensed yet (unlimited/not activated).
    private Instant expiresAt;
}
