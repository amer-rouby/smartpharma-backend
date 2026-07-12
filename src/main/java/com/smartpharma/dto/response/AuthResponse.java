package com.smartpharma.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String role;
    private Long pharmacyId;
    private String pharmacyName;
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private LocalDateTime expiresAt;
    private Integer sessionTimeout;
    private Integer warningThreshold;
    private Integer maxExtensions;
    private Integer remainingExtensions;
}
