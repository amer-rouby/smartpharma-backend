package com.smartpharma.einvoice.service.impl;

import com.smartpharma.einvoice.entity.enums.EtaEnvironment;
import com.smartpharma.common.entity.Pharmacy;
import com.smartpharma.common.exception.LocalizedException;
import com.smartpharma.common.repository.PharmacyRepository;
import com.smartpharma.einvoice.dto.request.EtaPosDeviceRequest;
import com.smartpharma.einvoice.dto.request.EtaSettingsRequest;
import com.smartpharma.einvoice.dto.response.EtaPosDeviceResponse;
import com.smartpharma.einvoice.dto.response.EtaSettingsResponse;
import com.smartpharma.einvoice.entity.EtaPosDevice;
import com.smartpharma.einvoice.entity.EtaTaxpayerProfile;
import com.smartpharma.einvoice.repository.EtaPosDeviceRepository;
import com.smartpharma.einvoice.repository.EtaTaxpayerProfileRepository;
import com.smartpharma.einvoice.service.EtaSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EtaSettingsServiceImpl implements EtaSettingsService {

    private final EtaTaxpayerProfileRepository profileRepository;
    private final EtaPosDeviceRepository deviceRepository;
    private final PharmacyRepository pharmacyRepository;
    private final EtaSecretCipher cipher;
    private final EtaApiClient apiClient;
    private final EtaCredentialsResolver credentialsResolver;

    @Override
    @Transactional(readOnly = true)
    public EtaSettingsResponse getSettings(Long pharmacyId) {
        return profileRepository.findByPharmacyId(pharmacyId)
                .map(p -> EtaSettingsResponse.fromEntity(p, cipher.isConfigured()))
                .orElseGet(() -> EtaSettingsResponse.builder()
                        .environment(EtaEnvironment.PREPROD.name())
                        .branchCode("0")
                        .credentialsKeyConfigured(cipher.isConfigured())
                        .build());
    }

    @Override
    @Transactional
    public EtaSettingsResponse saveSettings(Long pharmacyId, EtaSettingsRequest request) {
        EtaTaxpayerProfile profile = profileRepository.findByPharmacyId(pharmacyId)
                .orElseGet(() -> EtaTaxpayerProfile.builder().pharmacy(pharmacy(pharmacyId)).build());

        // Formats and required fields are enforced by @Valid on EtaSettingsRequest.
        profile.setEnvironment(EtaEnvironment.valueOf(request.getEnvironment()));
        profile.setRin(trim(request.getRin()));
        profile.setCompanyTradeName(trim(request.getCompanyTradeName()));
        profile.setBranchCode(trim(request.getBranchCode()));
        profile.setActivityCode(trim(request.getActivityCode()));
        profile.setGovernate(trim(request.getGovernate()));
        profile.setRegionCity(trim(request.getRegionCity()));
        profile.setStreet(trim(request.getStreet()));
        profile.setBuildingNumber(trim(request.getBuildingNumber()));
        profile.setPostalCode(trim(request.getPostalCode()));
        String taxSubtype = trim(request.getDefaultTaxSubtype());
        profile.setDefaultTaxSubtype(taxSubtype);
        profile.setDefaultTaxRate("V010".equals(taxSubtype) ? request.getDefaultTaxRate() : null);
        profile.setClientId(trim(request.getClientId()));
        if (request.getClientSecret() != null && !request.getClientSecret().isBlank()) {
            profile.setClientSecretEncrypted(cipher.encrypt(request.getClientSecret().trim()));
        }
        return EtaSettingsResponse.fromEntity(profileRepository.save(profile), cipher.isConfigured());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EtaPosDeviceResponse> getDevices(Long pharmacyId) {
        return deviceRepository.findByPharmacyId(pharmacyId).stream().map(EtaPosDeviceResponse::fromEntity).toList();
    }

    @Override
    @Transactional
    public EtaPosDeviceResponse createDevice(Long pharmacyId, EtaPosDeviceRequest request) {
        if (request.getPresharedKey() == null || request.getPresharedKey().isBlank()) {
            throw invalid("ETA_DEVICE_KEY_REQUIRED", "POS pre-shared key is required");
        }
        EtaPosDevice device = EtaPosDevice.builder()
                .pharmacy(pharmacy(pharmacyId))
                .presharedKeyEncrypted(cipher.encrypt(request.getPresharedKey().trim()))
                .active(request.getActive() == null || request.getActive())
                .build();
        applyDeviceFields(device, request);
        ensureSingleActive(pharmacyId, device);
        return EtaPosDeviceResponse.fromEntity(deviceRepository.save(device));
    }

    @Override
    @Transactional
    public EtaPosDeviceResponse updateDevice(Long pharmacyId, Long deviceId, EtaPosDeviceRequest request) {
        EtaPosDevice device = deviceRepository.findByIdAndPharmacyId(deviceId, pharmacyId)
                .orElseThrow(() -> new LocalizedException(HttpStatus.NOT_FOUND, "ETA_DEVICE_NOT_FOUND", "POS device not found"));
        String serial = trim(request.getSerialNumber());
        if (device.getLastReceiptUuid() != null && serial != null && !serial.equals(device.getSerialNumber())) {
            throw invalid("ETA_DEVICE_SERIAL_LOCKED",
                    "This device already issued receipts - register a new device instead of changing its serial");
        }
        applyDeviceFields(device, request);
        if (request.getPresharedKey() != null && !request.getPresharedKey().isBlank()) {
            device.setPresharedKeyEncrypted(cipher.encrypt(request.getPresharedKey().trim()));
        }
        if (request.getActive() != null) {
            device.setActive(request.getActive());
        }
        ensureSingleActive(pharmacyId, device);
        return EtaPosDeviceResponse.fromEntity(deviceRepository.save(device));
    }

    @Override
    @Transactional(readOnly = true)
    public String testConnection(Long pharmacyId, Long deviceId) {
        EtaPosDevice device = deviceRepository.findByIdAndPharmacyId(deviceId, pharmacyId)
                .orElseThrow(() -> new LocalizedException(HttpStatus.NOT_FOUND, "ETA_DEVICE_NOT_FOUND", "POS device not found"));
        EtaApiClient.PosCredentials credentials = credentialsResolver.resolve(device);
        apiClient.forget(credentials);
        try {
            apiClient.authenticate(credentials);
        } catch (EtaApiClient.EtaAuthenticationException e) {
            throw new LocalizedException(HttpStatus.BAD_GATEWAY, "ETA_AUTH_FAILED", e.getMessage(),
                    Map.of("detail", e.getMessage()));
        }
        return "Authenticated with ETA " + credentials.environment().name();
    }

    // Presence and lengths are enforced by @Valid on EtaPosDeviceRequest.
    private void applyDeviceFields(EtaPosDevice device, EtaPosDeviceRequest request) {
        device.setSerialNumber(trim(request.getSerialNumber()));
        device.setOsVersion(trim(request.getOsVersion()));
        device.setModelFramework(trim(request.getModelFramework()));
    }

    // The issuer needs exactly one active device per pharmacy to know which chain a sale goes on.
    private void ensureSingleActive(Long pharmacyId, EtaPosDevice device) {
        if (!Boolean.TRUE.equals(device.getActive())) {
            return;
        }
        boolean otherActive = deviceRepository.findActiveByPharmacyId(pharmacyId).stream()
                .anyMatch(d -> !d.getId().equals(device.getId()));
        if (otherActive) {
            throw invalid("ETA_DEVICE_ALREADY_ACTIVE", "Another POS device is active - deactivate it first");
        }
    }

    private Pharmacy pharmacy(Long pharmacyId) {
        return pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new LocalizedException(HttpStatus.NOT_FOUND, "PHARMACY_NOT_FOUND", "Pharmacy not found"));
    }

    private static LocalizedException invalid(String code, String message) {
        return new LocalizedException(HttpStatus.BAD_REQUEST, code, message);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
