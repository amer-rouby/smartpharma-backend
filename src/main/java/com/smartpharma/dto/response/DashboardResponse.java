// src/main/java/com/smartpharma/dto/response/DashboardResponse.java

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
public class DashboardResponse {

    private BigDecimal todayRevenue;
    private Long todayOrders;
    private BigDecimal todayAverageOrder;
    private Long totalProducts;
    private Long lowStockProducts;
    private Long outOfStockProducts;
    private BigDecimal inventoryValue;
    private Long expiringBatches;
    private Long expiredBatches;
    private List<TopProductDTO> topProducts;
    private List<RecentSaleDTO> recentSales;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductDTO {
        private Long productId;
        private String productName;
        private Long quantitySold;
        private BigDecimal totalRevenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentSaleDTO {
        private Long saleId;
        private String invoiceNumber;
        private BigDecimal totalAmount;
        private String transactionDate;
        private String paymentMethod;
    }
}