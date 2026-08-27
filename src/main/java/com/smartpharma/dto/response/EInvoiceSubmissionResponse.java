package com.smartpharma.dto.response;

import com.smartpharma.entity.EInvoiceSubmission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EInvoiceSubmissionResponse {
    private Long id;
    private Long saleTransactionId;
    private String status;
    private String etaUuid;
    private LocalDateTime submittedAt;
    private String errorMessage;
    private Integer retryCount;

    public static EInvoiceSubmissionResponse fromEntity(EInvoiceSubmission submission) {
        return EInvoiceSubmissionResponse.builder()
                .id(submission.getId())
                .saleTransactionId(submission.getSaleTransaction().getId())
                .status(submission.getStatus().name())
                .etaUuid(submission.getEtaUuid())
                .submittedAt(submission.getSubmittedAt())
                .errorMessage(submission.getErrorMessage())
                .retryCount(submission.getRetryCount())
                .build();
    }
}
