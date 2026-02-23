package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReportResponse {
    private BigDecimal totalStockValue;
    private Long totalItems;
    private Long lowStockItems;
    private Long expiredItems;
    private Long expiringSoonItems;
    private List<StockByCategoryDTO> stockByCategory;
    private List<StockItemDTO> lowStockProducts;
    private List<StockItemDTO> expiringProducts;

    @Data
    @Builder
    public static class StockByCategoryDTO {
        private String categoryName;
        private Long itemCount;
        private BigDecimal totalValue;
    }

    @Data
    @Builder
    public static class StockItemDTO {
        private Long productId;
        private String productName;
        private String batchNumber;
        private Integer currentStock;
        private Integer minStock;
        private String expiryDate;
        private Integer daysUntilExpiry;
    }
}