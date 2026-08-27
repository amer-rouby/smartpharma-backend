package com.smartpharma.service;

import com.smartpharma.dto.response.DashboardResponse;
import com.smartpharma.dto.response.SmartInsightsDTO;

public interface DashboardService {

    DashboardResponse getDashboardStats(Long pharmacyId);

    SmartInsightsDTO getSmartInsights(Long pharmacyId);
}