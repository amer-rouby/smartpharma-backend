package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendWhatsAppResponse {
    private boolean success;
    private String messageId;
    private String whatsAppUrl;
    private String phoneNumber;
    private String encodedMessage;
}