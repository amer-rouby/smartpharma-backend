package com.smartpharma.dto.settings.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacySettingsRequest {

    @NotBlank(message = "اسم الصيدلية مطلوب")
    @Size(max = 100)
    private String pharmacyName;

    @Size(max = 255)
    private String address;

    @Size(max = 50)
    private String phone;

    @Email(message = "البريد الإلكتروني غير صحيح")
    @Size(max = 100)
    private String email;

    @Size(max = 50)
    private String licenseNumber;

    @Size(max = 50)
    private String taxNumber;

    @Size(max = 100)
    private String commercialRegister;

    @Size(max = 255)
    private String logoUrl;

    @Size(max = 20)
    @Builder.Default
    private String currency = "EGP";

    @Size(max = 50)
    @Builder.Default
    private String timezone = "Africa/Cairo";

    @Size(max = 20)
    @Builder.Default
    private String dateFormat = "dd/MM/yyyy";

    @Size(max = 20)
    @Builder.Default
    private String timeFormat = "24h";

    @Builder.Default
    private Boolean emailNotifications = true;

    @Builder.Default
    private Boolean smsNotifications = false;

    @Builder.Default
    private Boolean lowStockAlerts = true;

    @Builder.Default
    private Boolean expiryAlerts = true;
}