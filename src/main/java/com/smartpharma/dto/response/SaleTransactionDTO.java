package com.smartpharma.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleTransactionDTO {
    private Long id;
    private String invoiceNumber;
    private BigDecimal subtotal;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private String paymentMethod;
    private String customerPhone;

    // ADD THIS LINE HERE
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transactionDate;

    private Long pharmacyId;
    private String pharmacyName;
    private List<SaleItemDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemDTO {
        private Long id;
        private Long productId;
        private String productName;
        private String barcode;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String category;
        private String batchNumber;
        private java.time.LocalDate expiryDate;
    }
}