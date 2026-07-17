package com.smartpharma.service;

import com.smartpharma.dto.response.SendWhatsAppResponse;

public interface WhatsAppClientService {
    SendWhatsAppResponse sendMessage(String phoneNumber, String message);
}