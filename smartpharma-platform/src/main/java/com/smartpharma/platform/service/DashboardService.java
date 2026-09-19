package com.smartpharma.platform.service;

import com.smartpharma.platform.dto.response.DashboardResponse;
import com.smartpharma.platform.dto.response.SmartInsightsDTO;

public interface DashboardService {

    DashboardResponse getDashboardStats(Long pharmacyId);

    SmartInsightsDTO getSmartInsights(Long pharmacyId);
}