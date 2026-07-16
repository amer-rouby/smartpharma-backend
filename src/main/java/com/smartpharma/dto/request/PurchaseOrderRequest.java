package com.smartpharma.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseOrderRequest {

    @NotNull(message = "Supplier is required")
    private Long supplierId;

    @NotNull(message = "Order date is required")
    private LocalDate orderDate;

    private LocalDate expectedDeliveryDate;

    @Size(max = 20, message = "Invalid priority value")
    @Builder.Default
    private String priority = "NORMAL";

    @Size(max = 50, message = "Payment terms must not exceed 50 characters")
    private String paymentTerms;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @Size(max = 50, message = "Invalid source type")
    private String sourceType; // MANUAL, PREDICTION, AUTO

    private Long sourceId; // predictionId if from prediction

    @NotEmpty(message = "At least one product must be added")
    private List<PurchaseOrderItemRequest> items;
}