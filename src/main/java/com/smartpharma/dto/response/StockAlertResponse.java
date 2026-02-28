package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertResponse {

    private Long id;
    private Long pharmacyId;
    private Long productId;
    private String productName;
    private Long batchId;
    private String batchNumber;

    private String alertType;
    private String title;
    private String message;
    private String severity;
    private String status;
    private String metadata;

    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;

    public static StockAlertResponse fromEntity(com.smartpharma.entity.StockAlert alert) {
        return StockAlertResponse.builder()
                .id(alert.getId())
                .pharmacyId(alert.getPharmacy().getId())
                .productId(alert.getProduct() != null ? alert.getProduct().getId() : null)
                .productName(alert.getProduct() != null ? alert.getProduct().getName() : null)
                .batchId(alert.getBatch() != null ? alert.getBatch().getId() : null)
                .batchNumber(alert.getBatch() != null ? alert.getBatch().getBatchNumber() : null)
                .alertType(alert.getAlertType().name())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .metadata(alert.getMetadata())
                .createdAt(alert.getCreatedAt())
                .readAt(alert.getReadAt())
                .resolvedAt(alert.getResolvedAt())
                .resolvedBy(alert.getResolvedBy() != null ? alert.getResolvedBy().getId() : null)
                .build();
    }
}