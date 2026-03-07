package com.smartpharma.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseOrderItemRequest {

    @NotNull(message = "المنتج مطلوب")
    private Long productId;

    @NotNull(message = "الكمية مطلوبة")
    @Min(value = 1, message = "الكمية يجب أن تكون 1 على الأقل")
    private Integer quantity;

    @NotNull(message = "سعر الوحدة مطلوب")
    @Min(value = 0, message = "سعر الوحدة يجب أن يكون 0 أو أكثر")
    private BigDecimal unitPrice;

    @Size(max = 500, message = "الملاحظات يجب ألا تتجاوز 500 حرف")
    private String notes;
}