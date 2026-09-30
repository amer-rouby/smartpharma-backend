package com.smartpharma.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// Changes many sell prices at once, either by a percentage (all products, a
// category, or selected ones) or from a price list (e.g. an official price
// update). apply=false only previews the result - nothing is saved.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPriceUpdateRequest {

    @NotNull(message = "Mode is required")
    @Pattern(regexp = "PERCENT|PRICE_LIST", message = "Mode must be PERCENT or PRICE_LIST")
    private String mode;

    // PERCENT: +10 raises prices by 10%, -5 lowers them by 5%.
    @DecimalMin(value = "-90", message = "Prices can't be lowered by more than 90%")
    @DecimalMax(value = "500", message = "Prices can't be raised by more than 500%")
    private BigDecimal percent;

    // PERCENT scope: selected products, else a category, else every product.
    @Size(max = 5000)
    private List<Long> productIds;

    private String category;

    // PERCENT: round each new price to a multiple of this (e.g. 0.25).
    @DecimalMin(value = "0.01", message = "Rounding step must be at least 0.01")
    private BigDecimal roundTo;

    // PRICE_LIST: one entry per product, matched by productId or barcode.
    @Valid
    @Size(max = 5000, message = "A price list can have at most 5000 lines")
    private List<PriceListItem> items;

    private boolean apply;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceListItem {
        private Long productId;

        @Size(max = 100)
        private String barcode;

        @NotNull(message = "New price is required")
        @DecimalMin(value = "0.01", message = "New price must be at least 0.01")
        private BigDecimal sellPrice;
    }
}
