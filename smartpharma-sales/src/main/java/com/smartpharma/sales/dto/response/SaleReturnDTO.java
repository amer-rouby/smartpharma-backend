package com.smartpharma.sales.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartpharma.sales.entity.SaleReturn;
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
public class SaleReturnDTO {
    private Long id;
    private Long saleId;
    private Integer returnNumber;
    private BigDecimal itemsTotal;
    private BigDecimal discountShare;
    private BigDecimal refundAmount;
    private boolean restocked;
    private String reason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long saleItemId;
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    public static SaleReturnDTO fromEntity(SaleReturn saleReturn) {
        return SaleReturnDTO.builder()
                .id(saleReturn.getId())
                .saleId(saleReturn.getSaleTransaction().getId())
                .returnNumber(saleReturn.getReturnNumber())
                .itemsTotal(saleReturn.getItemsTotal())
                .discountShare(saleReturn.getDiscountShare())
                .refundAmount(saleReturn.getRefundAmount())
                .restocked(saleReturn.isRestocked())
                .reason(saleReturn.getReason())
                .createdAt(saleReturn.getCreatedAt())
                .items(saleReturn.getItems().stream()
                        .map(item -> Item.builder()
                                .saleItemId(item.getSaleItem().getId())
                                .productId(item.getSaleItem().getProduct().getId())
                                .productName(item.getSaleItem().getProduct().getName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build())
                        .toList())
                .build();
    }
}
