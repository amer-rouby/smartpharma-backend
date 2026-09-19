package com.smartpharma.license.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LicenseGenerateRequest {
    @NotNull
    private Long pharmacyId;

    @Min(1)
    private int months;
}
