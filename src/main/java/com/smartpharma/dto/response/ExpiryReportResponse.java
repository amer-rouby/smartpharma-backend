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
    private Long urgentExpiring;
    private Long warningExpiring;
    private Long okExpiring;
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
        private String status;
        private Double estimatedValue;
    }
}