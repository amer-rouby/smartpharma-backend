package com.smartpharma.einvoice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Field limits mirror the ETA receipt v1.2 seller/branchAddress structure.
// clientSecret: null keeps the stored one, so the UI never has to read it back.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtaSettingsRequest {

    @NotBlank(message = "Environment is required")
    @Pattern(regexp = "PREPROD|PROD", message = "Environment must be PREPROD or PROD")
    private String environment;

    @NotBlank(message = "Tax registration number is required")
    @Pattern(regexp = "\\d{9}", message = "Tax registration number must be 9 digits")
    private String rin;

    @NotBlank(message = "Company trade name is required")
    @Size(max = 200)
    private String companyTradeName;

    @NotBlank(message = "Branch code is required")
    @Size(max = 50)
    private String branchCode;

    @NotBlank(message = "Activity code is required")
    @Size(max = 10)
    private String activityCode;

    @NotBlank(message = "Governorate is required")
    @Size(max = 100)
    private String governate;

    @NotBlank(message = "Region/city is required")
    @Size(max = 100)
    private String regionCity;

    @NotBlank(message = "Street is required")
    @Size(max = 200)
    private String street;

    @NotBlank(message = "Building number is required")
    @Size(max = 100)
    private String buildingNumber;

    @Size(max = 30)
    private String postalCode;

    @NotBlank(message = "ETA client ID is required")
    @Size(max = 100)
    private String clientId;

    @Size(max = 100)
    private String clientSecret;
}
