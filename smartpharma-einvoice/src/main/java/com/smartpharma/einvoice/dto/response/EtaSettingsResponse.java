package com.smartpharma.einvoice.dto.response;

import com.smartpharma.einvoice.entity.EtaTaxpayerProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Never carries the client secret - only whether one is stored.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtaSettingsResponse {
    private String environment;
    private String rin;
    private String companyTradeName;
    private String branchCode;
    private String activityCode;
    private String governate;
    private String regionCity;
    private String street;
    private String buildingNumber;
    private String postalCode;
    private String defaultTaxSubtype;
    private BigDecimal defaultTaxRate;
    private String clientId;
    private boolean clientSecretSet;
    private boolean credentialsKeyConfigured;

    public static EtaSettingsResponse fromEntity(EtaTaxpayerProfile p, boolean credentialsKeyConfigured) {
        return EtaSettingsResponse.builder()
                .environment(p.getEnvironment().name())
                .rin(p.getRin())
                .companyTradeName(p.getCompanyTradeName())
                .branchCode(p.getBranchCode())
                .activityCode(p.getActivityCode())
                .governate(p.getGovernate())
                .regionCity(p.getRegionCity())
                .street(p.getStreet())
                .buildingNumber(p.getBuildingNumber())
                .postalCode(p.getPostalCode())
                .defaultTaxSubtype(p.getDefaultTaxSubtype())
                .defaultTaxRate(p.getDefaultTaxRate())
                .clientId(p.getClientId())
                .clientSecretSet(p.getClientSecretEncrypted() != null)
                .credentialsKeyConfigured(credentialsKeyConfigured)
                .build();
    }
}
