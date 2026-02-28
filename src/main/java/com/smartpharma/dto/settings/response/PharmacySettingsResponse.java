package com.smartpharma.dto.settings.response;

import com.smartpharma.entity.settings.PharmacySettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacySettingsResponse {

    private Long id;
    private Long pharmacyId;
    private String pharmacyName;
    private String address;
    private String phone;
    private String email;
    private String licenseNumber;
    private String taxNumber;
    private String commercialRegister;
    private String logoUrl;
    private String currency;
    private String timezone;
    private String dateFormat;
    private String timeFormat;
    private Boolean emailNotifications;
    private Boolean smsNotifications;
    private Boolean lowStockAlerts;
    private Boolean expiryAlerts;
    private LocalDateTime updatedAt;

    public static PharmacySettingsResponse fromEntity(PharmacySettings settings, String pharmacyName) {
        return PharmacySettingsResponse.builder()
                .id(settings.getId())
                .pharmacyId(settings.getPharmacy().getId())
                .pharmacyName(pharmacyName)
                .address(settings.getAddress())
                .phone(settings.getPhone())
                .email(settings.getEmail())
                .licenseNumber(settings.getLicenseNumber())
                .taxNumber(settings.getTaxNumber())
                .commercialRegister(settings.getCommercialRegister())
                .logoUrl(settings.getLogoUrl())
                .currency(settings.getCurrency())
                .timezone(settings.getTimezone())
                .dateFormat(settings.getDateFormat())
                .timeFormat(settings.getTimeFormat())
                .emailNotifications(settings.getEmailNotifications())
                .smsNotifications(settings.getSmsNotifications())
                .lowStockAlerts(settings.getLowStockAlerts())
                .expiryAlerts(settings.getExpiryAlerts())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}