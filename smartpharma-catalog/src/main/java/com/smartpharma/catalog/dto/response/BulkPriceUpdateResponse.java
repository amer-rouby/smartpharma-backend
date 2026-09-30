package com.smartpharma.catalog.dto.response;

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
public class BulkPriceUpdateResponse {
    // false = preview only, nothing was saved.
    private boolean applied;
    private int changedCount;
    private int unchangedCount;
    private List<PriceChange> changes;
    // Price-list lines whose barcode/productId matched no product of this pharmacy.
    private List<String> notFound;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceChange {
        private Long productId;
        private String productName;
        private String barcode;
        private BigDecimal oldPrice;
        private BigDecimal newPrice;
        // New sell price is below the product's buy price.
        private boolean belowCost;
    }
}
