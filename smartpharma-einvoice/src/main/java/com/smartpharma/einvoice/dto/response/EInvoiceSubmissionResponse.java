package com.smartpharma.einvoice.dto.response;

import com.smartpharma.einvoice.entity.EInvoiceSubmission;
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
    private String receiptNumber;
    private String dateTimeIssued;
    private String qrContent;
    private String longId;
    private String submissionUuid;

    public static EInvoiceSubmissionResponse fromEntity(EInvoiceSubmission submission) {
        return EInvoiceSubmissionResponse.builder()
                .id(submission.getId())
                .saleTransactionId(submission.getSaleTransaction().getId())
                .status(submission.getStatus().name())
                .etaUuid(submission.getEtaUuid())
                .submittedAt(submission.getSubmittedAt())
                .errorMessage(submission.getErrorMessage())
                .retryCount(submission.getRetryCount())
                .receiptNumber(submission.getReceiptNumber())
                .dateTimeIssued(submission.getDateTimeIssued())
                .qrContent(submission.getQrContent())
                .longId(submission.getLongId())
                .submissionUuid(submission.getSubmissionUuid())
                .build();
    }
}
