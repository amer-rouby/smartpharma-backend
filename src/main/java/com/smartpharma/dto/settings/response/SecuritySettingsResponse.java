package com.smartpharma.dto.settings.response;

import com.smartpharma.entity.settings.SecuritySettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuritySettingsResponse {

    private Long id;
    private Long userId;
    private Boolean twoFactorEnabled;
    private Integer sessionTimeoutMinutes;
    private Boolean requirePasswordChange;
    private LocalDateTime lastPasswordChange;
    private Integer failedLoginAttempts;
    private Boolean accountLocked;
    private LocalDateTime accountLockedUntil;
    private String securityQuestion;
    private Boolean loginHistoryEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SecuritySettingsResponse fromEntity(SecuritySettings settings) {
        return SecuritySettingsResponse.builder()
                .id(settings.getId())
                .userId(settings.getUser().getId())
                .twoFactorEnabled(settings.getTwoFactorEnabled())
                .sessionTimeoutMinutes(settings.getSessionTimeoutMinutes())
                .requirePasswordChange(settings.getRequirePasswordChange())
                .lastPasswordChange(settings.getLastPasswordChange())
                .failedLoginAttempts(settings.getFailedLoginAttempts())
                .accountLocked(settings.getAccountLocked())
                .accountLockedUntil(settings.getAccountLockedUntil())
                .securityQuestion(settings.getSecurityQuestion())
                .loginHistoryEnabled(settings.getLoginHistoryEnabled())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}