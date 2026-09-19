package com.smartpharma.platform.service;

import com.smartpharma.platform.dto.response.PricingRecommendationDTO;

import java.util.List;

public interface PricingRecommendationService {
    List<PricingRecommendationDTO> getRecommendations(Long pharmacyId);
}
