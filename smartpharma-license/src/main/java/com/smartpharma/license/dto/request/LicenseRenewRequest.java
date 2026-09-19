package com.smartpharma.license.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LicenseRenewRequest {
    @NotBlank
    private String code;
}
