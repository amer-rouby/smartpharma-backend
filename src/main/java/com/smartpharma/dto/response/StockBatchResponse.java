package com.smartpharma.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockBatchResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Long pharmacyId;
    private String batchNumber;
    private Integer quantityCurrent;
    private Integer quantityInitial;
    private LocalDate expiryDate;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private String location;
    private String status;
    private Boolean isExpired;
    private Boolean isExpiringSoon;
    private LocalDateTime createdAt;
}