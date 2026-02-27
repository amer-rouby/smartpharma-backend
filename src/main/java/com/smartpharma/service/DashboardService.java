package com.smartpharma.service;

import com.smartpharma.dto.response.DashboardResponse;

public interface DashboardService {

    DashboardResponse getDashboardStats(Long pharmacyId);
}