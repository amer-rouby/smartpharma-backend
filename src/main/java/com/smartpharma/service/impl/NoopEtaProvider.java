package com.smartpharma.service.impl;

import com.smartpharma.config.EtaConfig;
import com.smartpharma.entity.SaleTransaction;
import com.smartpharma.service.EtaIntegrationService;
import com.smartpharma.service.EtaSubmissionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Default ETA provider. No real Egyptian Tax Authority API call is made -
// this always reports a clear error rather than a fabricated success, per
// the "no fake success responses" requirement. Swap in a real
// EtaIntegrationService implementation once ETA_CLIENT_ID/ETA_CLIENT_SECRET
// are actually available - see docs/eta-integration.md.
@Service
@RequiredArgsConstructor
public class NoopEtaProvider implements EtaIntegrationService {

    private final EtaConfig etaConfig;

    @Override
    public EtaSubmissionResult submit(SaleTransaction sale) {
        if (!etaConfig.isConfigured()) {
            return EtaSubmissionResult.error("ETA credentials not configured");
        }
        return EtaSubmissionResult.error(
                "ETA credentials are set but no real ETA integration is implemented yet");
    }
}
