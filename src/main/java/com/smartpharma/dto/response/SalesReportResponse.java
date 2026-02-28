package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportResponse {
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private BigDecimal averageOrder;
    private Long totalItems;
    private Map<String, BigDecimal> revenueByPaymentMethod;
    private Map<String, Long> ordersByDay;
    private List<TopProductDTO> topProducts;
    private List<DailySalesDTO> dailySales;

    @Data
    @Builder
    public static class TopProductDTO {
        private Long productId;
        private String productName;
        private Long quantitySold;
        private BigDecimal totalRevenue;
    }

    @Data
    @Builder
    public static class DailySalesDTO {
        private String date;
        private BigDecimal revenue;
        private Long orders;
    }
}