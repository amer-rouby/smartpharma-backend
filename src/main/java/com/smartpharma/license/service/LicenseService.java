package com.smartpharma.license.service;

import com.smartpharma.license.dto.response.LicenseGenerateResponse;
import com.smartpharma.license.dto.response.LicenseStatusResponse;

public interface LicenseService {
    LicenseStatusResponse getStatus(Long pharmacyId);

    LicenseStatusResponse renew(Long pharmacyId, String code);

    // Vendor-only: signs a new activation/renewal code with the private key.
    // Only meaningful on the internal, never-shipped instance that has
    // license.private-key-path configured - see LicenseServiceImpl.
    LicenseGenerateResponse generateCode(Long pharmacyId, int months);
}
