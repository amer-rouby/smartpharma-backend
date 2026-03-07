package com.smartpharma.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseOrderResponse {

    private Long id;
    private String orderNumber;
    private Long pharmacyId;
    private Long supplierId;
    private String supplierName;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private BigDecimal totalAmount;
    private String status;
    private String priority;
    private String paymentTerms;
    private String notes;
    private String sourceType;
    private Long sourceId;
    private Long createdById;
    private String createdByFullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PurchaseOrderItemResponse> items;

    public static PurchaseOrderResponse fromEntity(com.smartpharma.entity.PurchaseOrder po) {
        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .orderNumber(po.getOrderNumber())
                .pharmacyId(po.getPharmacy().getId())
                .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
                .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : null)
                .orderDate(po.getOrderDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .actualDeliveryDate(po.getActualDeliveryDate())
                .totalAmount(po.getTotalAmount())
                .status(po.getStatus())
                .priority(po.getPriority())
                .paymentTerms(po.getPaymentTerms())
                .notes(po.getNotes())
                .sourceType(po.getSourceType())
                .sourceId(po.getSourceId())
                .createdById(po.getCreatedBy() != null ? po.getCreatedBy().getId() : null)
                .createdByFullName(po.getCreatedBy() != null ? po.getCreatedBy().getFullName() : null)
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .items(po.getItems() != null ? po.getItems().stream()
                        .map(PurchaseOrderItemResponse::fromEntity)
                        .collect(Collectors.toList()) : null)
                .build();
    }
}