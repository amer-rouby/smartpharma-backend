package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppMessageResponse {
    private String phoneNumber;
    private String message;
    private String encodedMessage;
    private String orderNumber;
    private String whatsAppUrl;
}
