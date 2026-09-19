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
public class LicenseGenerateResponse {
    private String code;
    private Long pharmacyId;
    private Instant expiresAt;
}
