package com.smartpharma.dto.settings.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuritySettingsRequest {

    private Boolean twoFactorEnabled;

    @Min(value = 5, message = "Session timeout must be at least 5 minutes")
    private Integer sessionTimeoutMinutes;

    private Boolean requirePasswordChange;

    @Size(min = 3, max = 200, message = "Security question must be between 3 and 200 characters")
    private String securityQuestion;

    @Size(min = 3, max = 100, message = "Security answer must be between 3 and 100 characters")
    private String securityAnswer;

    private Boolean loginHistoryEnabled;
}