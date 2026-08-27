package com.smartpharma.controller;

import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.PricingRecommendationDTO;
import com.smartpharma.service.PricingRecommendationService;
import com.smartpharma.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pricing-recommendations")
@RequiredArgsConstructor
@Slf4j
public class PricingRecommendationController {

    private final PricingRecommendationService pricingRecommendationService;

    // No try/catch - a disabled feature flag throws LocalizedException, handled
    // globally with the correct status and translatable error code.
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PricingRecommendationDTO>>> getRecommendations() {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        List<PricingRecommendationDTO> recommendations = pricingRecommendationService.getRecommendations(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(recommendations));
    }
}
