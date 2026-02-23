// src/main/java/com/smartpharma/service/DashboardService.java

package com.smartpharma.service;

import com.smartpharma.dto.response.DashboardResponse;

public interface DashboardService {

    DashboardResponse getDashboardStats(Long pharmacyId);
}