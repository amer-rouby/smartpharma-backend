package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.smartpharma.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {

    private Long id;
    private Long pharmacyId;
    private ExpenseCategory category;
    private String categoryArabic;
    private String title;
    private String description;
    private BigDecimal amount;
    private LocalDateTime expenseDate;
    private String paymentMethod;
    private String referenceNumber;
    private String attachmentUrl;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}