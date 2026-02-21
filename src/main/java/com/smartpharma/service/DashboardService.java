package com.smartpharma.service;

import java.util.Map;

public interface DashboardService {

    Map<String, Object> getDashboardStats(Long pharmacyId);
}