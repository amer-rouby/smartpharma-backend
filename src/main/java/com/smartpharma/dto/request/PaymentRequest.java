package com.smartpharma.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Pharmacy ID is required")
    private Long pharmacyId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    @Pattern(regexp = "^(CASH|VISA|INSTAPAY|FAWRY|WALLET|BANK_TRANSFER)$", message = "Invalid payment method")
    private String paymentMethod;

    @Size(max = 100, message = "Customer name too long")
    private String customerName;

    @Pattern(regexp = "^(|01[0-9]{9})$", message = "Invalid phone number format")
    private String customerPhone;

    @Email(message = "Invalid email format", regexp = "^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$|^$")
    private String customerEmail;

    private String description;

    @Min(value = 1)
    private Integer expiryHours;
}