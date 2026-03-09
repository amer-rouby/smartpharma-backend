package com.smartpharma.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private String referenceNumber;
    private String paymentMethod;
    private BigDecimal amount;
    private String status;
    private String customerName;
    private String customerPhone;
    private String description;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private String message;
}