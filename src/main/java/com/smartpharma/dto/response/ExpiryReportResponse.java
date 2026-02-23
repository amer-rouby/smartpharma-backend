package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryReportResponse {
    private Long totalExpiring;
    private Long urgentExpiring;      // خلال 7 أيام
    private Long warningExpiring;     // خلال 30 يوم
    private Long okExpiring;          // خلال 90 يوم
    private List<ExpiringProductDTO> expiringProducts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpiringProductDTO {
        private Long productId;
        private String productName;
        private String batchNumber;
        private String expiryDate;
        private Integer daysUntilExpiry;
        private Integer currentStock;
        private String status;  // URGENT, WARNING, OK
        private Double estimatedValue;
    }
}