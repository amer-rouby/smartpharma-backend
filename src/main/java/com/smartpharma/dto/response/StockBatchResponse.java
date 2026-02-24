// src/main/java/com/smartpharma/dto/response/StockBatchResponse.java

package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBatchResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Long pharmacyId;

    private String batchNumber;
    private Integer quantityCurrent;
    private Integer quantityInitial;

    private LocalDate expiryDate;
    private LocalDate productionDate;  // ← ← ← تأكد إن ده موجود

    private BigDecimal buyPrice;
    private BigDecimal sellPrice;

    private String location;
    private String shelf;
    private String warehouse;
    private String notes;

    private String status;
    private Boolean isExpired;
    private Boolean isExpiringSoon;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}