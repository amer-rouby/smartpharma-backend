package com.smartpharma.einvoice.service;

import com.smartpharma.einvoice.dto.request.EtaPosDeviceRequest;
import com.smartpharma.einvoice.dto.request.EtaSettingsRequest;
import com.smartpharma.einvoice.dto.response.EtaPosDeviceResponse;
import com.smartpharma.einvoice.dto.response.EtaSettingsResponse;

import java.util.List;

public interface EtaSettingsService {
    EtaSettingsResponse getSettings(Long pharmacyId);

    EtaSettingsResponse saveSettings(Long pharmacyId, EtaSettingsRequest request);

    List<EtaPosDeviceResponse> getDevices(Long pharmacyId);

    EtaPosDeviceResponse createDevice(Long pharmacyId, EtaPosDeviceRequest request);

    EtaPosDeviceResponse updateDevice(Long pharmacyId, Long deviceId, EtaPosDeviceRequest request);

    // Authenticates the device against ETA with the saved credentials.
    String testConnection(Long pharmacyId, Long deviceId);
}
