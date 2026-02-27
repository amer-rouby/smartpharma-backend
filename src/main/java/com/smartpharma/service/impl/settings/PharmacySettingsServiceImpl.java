package com.smartpharma.service.impl.settings;

import com.smartpharma.dto.settings.request.PharmacySettingsRequest;
import com.smartpharma.dto.settings.response.PharmacySettingsResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.settings.PharmacySettings;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.settings.PharmacySettingsRepository;
import com.smartpharma.service.settings.PharmacySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacySettingsServiceImpl implements PharmacySettingsService {

    private final PharmacySettingsRepository settingsRepository;
    private final PharmacyRepository pharmacyRepository;

    @Override
    @Transactional(readOnly = true)
    public PharmacySettingsResponse getSettings(Long pharmacyId) {
        PharmacySettings settings = settingsRepository.findByPharmacyId(pharmacyId)
                .orElseGet(() -> createDefaultSettings(pharmacyId));

        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        return PharmacySettingsResponse.fromEntity(settings, pharmacy.getName());
    }

    @Override
    @Transactional
    public PharmacySettingsResponse updateSettings(Long pharmacyId, PharmacySettingsRequest request) {
        PharmacySettings settings = settingsRepository.findByPharmacyId(pharmacyId)
                .orElseGet(() -> createDefaultSettings(pharmacyId));

        settings.setAddress(request.getAddress());
        settings.setPhone(request.getPhone());
        settings.setEmail(request.getEmail());
        settings.setLicenseNumber(request.getLicenseNumber());
        settings.setTaxNumber(request.getTaxNumber());
        settings.setCommercialRegister(request.getCommercialRegister());
        settings.setLogoUrl(request.getLogoUrl());
        settings.setCurrency(request.getCurrency());
        settings.setTimezone(request.getTimezone());
        settings.setDateFormat(request.getDateFormat());
        settings.setTimeFormat(request.getTimeFormat());
        settings.setEmailNotifications(request.getEmailNotifications());
        settings.setSmsNotifications(request.getSmsNotifications());
        settings.setLowStockAlerts(request.getLowStockAlerts());
        settings.setExpiryAlerts(request.getExpiryAlerts());

        if (request.getPharmacyName() != null && !request.getPharmacyName().isBlank()) {
            Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                    .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
            pharmacy.setName(request.getPharmacyName());
            pharmacyRepository.save(pharmacy);
        }

        PharmacySettings saved = settingsRepository.save(settings);
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        log.info("Pharmacy settings updated for pharmacyId: {}", pharmacyId);
        return PharmacySettingsResponse.fromEntity(saved, pharmacy.getName());
    }

    private PharmacySettings createDefaultSettings(Long pharmacyId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        return PharmacySettings.builder()
                .pharmacy(pharmacy)
                .currency("EGP")
                .timezone("Africa/Cairo")
                .dateFormat("dd/MM/yyyy")
                .timeFormat("24h")
                .emailNotifications(true)
                .smsNotifications(false)
                .lowStockAlerts(true)
                .expiryAlerts(true)
                .build();
    }
}