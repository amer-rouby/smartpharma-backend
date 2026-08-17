package com.smartpharma.dto.response;

import jakarta.persistence.EntityNotFoundException;
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

    // Referenced products/batches can be hard-deleted after the alert row was
    // created, leaving a stale FK - Hibernate throws EntityNotFoundException
    // on first access of that lazy proxy, so treat it the same as "not set".
    public static StockAlertResponse fromEntity(com.smartpharma.entity.StockAlert alert) {
        Long productId = null;
        String productName = null;
        try {
            if (alert.getProduct() != null) {
                productId = alert.getProduct().getId();
                productName = alert.getProduct().getName();
            }
        } catch (EntityNotFoundException e) {
            // deleted product, leave productId/productName null
        }

        Long batchId = null;
        String batchNumber = null;
        try {
            if (alert.getBatch() != null) {
                batchId = alert.getBatch().getId();
                batchNumber = alert.getBatch().getBatchNumber();
            }
        } catch (EntityNotFoundException e) {
            // deleted batch, leave batchId/batchNumber null
        }

        return StockAlertResponse.builder()
                .id(alert.getId())
                .pharmacyId(alert.getPharmacy().getId())
                .productId(productId)
                .productName(productName)
                .batchId(batchId)
                .batchNumber(batchNumber)
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