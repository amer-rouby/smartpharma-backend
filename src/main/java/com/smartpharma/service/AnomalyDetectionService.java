package com.smartpharma.service;

import com.smartpharma.dto.response.AnomalyDetectionResponse;
import com.smartpharma.entity.AnomalyDetection;
import org.springframework.data.domain.Page;

public interface AnomalyDetectionService {

    void runDetectionForAllPharmacies();

    void runDetectionForPharmacy(Long pharmacyId);

    Page<AnomalyDetectionResponse> getAnomalies(Long pharmacyId, AnomalyDetection.Status status,
                                                  AnomalyDetection.Type type, int page, int size);

    long countByStatus(Long pharmacyId, AnomalyDetection.Status status);

    AnomalyDetectionResponse markReviewed(Long id, Long pharmacyId, Long userId);

    AnomalyDetectionResponse dismiss(Long id, Long pharmacyId, Long userId);
}
