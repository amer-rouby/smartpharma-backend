package com.smartpharma.dto.settings.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupResponse {
    /** Base32 secret, shown once for manual entry into an authenticator app. */
    private String secret;
    /** otpauth:// URI - the frontend renders this as a QR code (no image generated server-side). */
    private String otpAuthUrl;
}
