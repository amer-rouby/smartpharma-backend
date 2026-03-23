package com.smartpharma.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long paymentId;
    private String referenceNumber;
    private String paymentMethod;
    private BigDecimal amount;
    private String status;
    private String message;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}