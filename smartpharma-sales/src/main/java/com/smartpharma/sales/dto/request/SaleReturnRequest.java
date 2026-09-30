package com.smartpharma.sales.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturnRequest {

    @NotEmpty(message = "Choose at least one item to return")
    @Valid
    private List<Item> items;

    @Size(max = 500, message = "Reason can't exceed 500 characters")
    private String reason;

    // False for damaged/expired goods that shouldn't go back on the shelf.
    private boolean restock = true;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @NotNull(message = "Sale item is required")
        private Long saleItemId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
