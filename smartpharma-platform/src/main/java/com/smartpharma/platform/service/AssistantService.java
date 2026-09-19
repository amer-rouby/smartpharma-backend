package com.smartpharma.platform.service;

import com.smartpharma.platform.dto.response.AssistantAnswer;

public interface AssistantService {
    AssistantAnswer ask(String query, Long pharmacyId);
}
