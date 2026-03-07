package com.smartpharma.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseOrderRequest {

    @NotNull(message = "المورد مطلوب")
    private Long supplierId;

    @NotNull(message = "تاريخ الطلب مطلوب")
    private LocalDate orderDate;

    private LocalDate expectedDeliveryDate;

    @Size(max = 20, message = "الأولوية غير صحيحة")
    @Builder.Default
    private String priority = "NORMAL";

    @Size(max = 50, message = "شروط الدفع يجب ألا تتجاوز 50 حرف")
    private String paymentTerms;

    @Size(max = 1000, message = "الملاحظات يجب ألا تتجاوز 1000 حرف")
    private String notes;

    @Size(max = 50, message = "نوع المصدر غير صحيح")
    private String sourceType; // MANUAL, PREDICTION, AUTO

    private Long sourceId; // predictionId if from prediction

    @NotEmpty(message = "يجب إضافة منتج واحد على الأقل")
    private List<PurchaseOrderItemRequest> items;
}