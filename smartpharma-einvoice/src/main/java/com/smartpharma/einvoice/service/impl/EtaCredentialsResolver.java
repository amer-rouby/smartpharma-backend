package com.smartpharma.einvoice.service.impl;

import com.smartpharma.common.exception.LocalizedException;
import com.smartpharma.einvoice.entity.EtaPosDevice;
import com.smartpharma.einvoice.entity.EtaTaxpayerProfile;
import com.smartpharma.einvoice.repository.EtaTaxpayerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// Everything needed to call ETA as a given POS device: the pharmacy's ERP
// credentials plus the device's own identity, secrets decrypted. Shared by
// sending, status polling and the settings connection test so the three
// can't drift apart. Call inside a transaction (device.pharmacy is lazy).
@Component
@RequiredArgsConstructor
public class EtaCredentialsResolver {

    private final EtaTaxpayerProfileRepository profileRepository;
    private final EtaSecretCipher cipher;

    public EtaApiClient.PosCredentials resolve(EtaPosDevice device) {
        EtaTaxpayerProfile profile = profileRepository.findByPharmacyId(device.getPharmacy().getId())
                .orElseThrow(() -> new LocalizedException(HttpStatus.BAD_REQUEST, "ETA_SETTINGS_MISSING",
                        "ETA taxpayer settings are not saved"));
        if (profile.getClientId() == null || profile.getClientId().isBlank()
                || profile.getClientSecretEncrypted() == null) {
            throw new LocalizedException(HttpStatus.BAD_REQUEST, "ETA_CREDENTIALS_MISSING",
                    "ETA client ID / secret are not set");
        }
        return new EtaApiClient.PosCredentials(
                profile.getEnvironment(), profile.getClientId(), cipher.decrypt(profile.getClientSecretEncrypted()),
                device.getSerialNumber(), device.getOsVersion(), device.getModelFramework(),
                cipher.decrypt(device.getPresharedKeyEncrypted()));
    }
}
