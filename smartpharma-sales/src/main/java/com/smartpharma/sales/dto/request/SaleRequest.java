package com.smartpharma.sales.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequest {

    @NotNull(message = "Pharmacy ID is required")
    private Long pharmacyId;

    @NotEmpty(message = "At least one item is required")
    private List<SaleItemRequest> items;

    private String notes;

    private String customerPhone;

    // Egyptian national ID: 14 digits, the first being the century (2 = 1900s, 3 = 2000s).
    @Pattern(regexp = "^$|[23]\\d{13}", message = "National ID must be 14 digits starting with 2 or 3")
    private String buyerNationalId;

    @Size(max = 100, message = "Buyer name is too long")
    private String buyerName;

    private String prescriptionImageUrl;

    // Offline POS: a device-made UUID that makes retries idempotent, and
    // when the sale actually happened (only honoured together with
    // clientSaleId, and only within the offline window).
    @Pattern(regexp = "^$|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
            message = "Client sale ID must be a UUID")
    private String clientSaleId;

    private Instant soldAt;

    private String paymentMethod = "CASH";

    @DecimalMin(value = "0", message = "Discount cannot be negative")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be positive")
    private BigDecimal totalAmount;
}