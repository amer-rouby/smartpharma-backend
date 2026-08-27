package com.smartpharma.dto.response;

import com.smartpharma.entity.AnomalyDetection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDetectionResponse {
    private Long id;
    private String type;
    private String severity;
    private String status;
    private String description;
    private String relatedEntityType;
    private Long relatedEntityId;
    private String relatedEntityName;
    private LocalDateTime detectedAt;
    private String reviewedByName;
    private LocalDateTime reviewedAt;

    public static AnomalyDetectionResponse fromEntity(AnomalyDetection a) {
        return AnomalyDetectionResponse.builder()
                .id(a.getId())
                .type(a.getType().name())
                .severity(a.getSeverity().name())
                .status(a.getStatus().name())
                .description(a.getDescription())
                .relatedEntityType(a.getRelatedEntityType())
                .relatedEntityId(a.getRelatedEntityId())
                .relatedEntityName(a.getRelatedEntityName())
                .detectedAt(a.getDetectedAt())
                .reviewedByName(a.getReviewedBy() != null ? a.getReviewedBy().getFullName() : null)
                .reviewedAt(a.getReviewedAt())
                .build();
    }
}
