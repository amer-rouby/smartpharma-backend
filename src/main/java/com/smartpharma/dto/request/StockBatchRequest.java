package com.smartpharma.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StockBatchRequest {

    @NotNull
    private Long productId;

    @NotNull
    private String batchNumber;

    @NotNull
    private Integer quantityInitial;

    private Integer quantityCurrent;

    @NotNull
    private LocalDate expiryDate;

    private LocalDate productionDate;

    private BigDecimal buyPrice;
    private BigDecimal sellPrice;

    private String location;
    private String shelf;
    private String warehouse;

    private String notes;
    private String status;
}