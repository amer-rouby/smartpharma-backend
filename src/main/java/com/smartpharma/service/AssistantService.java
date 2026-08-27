package com.smartpharma.service;

import com.smartpharma.dto.response.AssistantAnswer;

public interface AssistantService {
    AssistantAnswer ask(String query, Long pharmacyId);
}
