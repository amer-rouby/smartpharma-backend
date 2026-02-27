package com.smartpharma.dto.response;

import com.smartpharma.entity.StockMovement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {

    private Long id;
    private Long batchId;
    private String productName;
    private String batchNumber;
    private String movementType;
    private Integer quantity;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private BigDecimal unitPrice;
    private BigDecimal totalValue;
    private String referenceNumber;
    private String reason;
    private String notes;
    private LocalDateTime movementDate;
    private Long userId;
    private String userName;
    private Long pharmacyId;

    public static StockMovementResponse fromEntity(StockMovement movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .batchId(movement.getBatch().getId())
                .productName(movement.getBatch().getProduct().getName())
                .batchNumber(movement.getBatch().getBatchNumber())
                .movementType(movement.getMovementType().name())
                .quantity(movement.getQuantity())
                .quantityBefore(movement.getQuantityBefore())
                .quantityAfter(movement.getQuantityAfter())
                .unitPrice(movement.getUnitPrice())
                .totalValue(movement.getTotalValue())
                .referenceNumber(movement.getReferenceNumber())
                .reason(movement.getReason())
                .notes(movement.getNotes())
                .movementDate(movement.getMovementDate())
                .userId(movement.getUser().getId())
                .userName(movement.getUser().getFullName())
                .pharmacyId(movement.getPharmacyId())
                .build();
    }
}