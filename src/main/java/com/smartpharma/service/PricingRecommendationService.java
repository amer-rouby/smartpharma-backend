package com.smartpharma.service;

import com.smartpharma.dto.response.PricingRecommendationDTO;

import java.util.List;

public interface PricingRecommendationService {
    List<PricingRecommendationDTO> getRecommendations(Long pharmacyId);
}
