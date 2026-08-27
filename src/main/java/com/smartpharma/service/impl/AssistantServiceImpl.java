package com.smartpharma.service.impl;

import com.smartpharma.dto.response.AssistantAnswer;
import com.smartpharma.exception.LocalizedException;
import com.smartpharma.service.AssistantProvider;
import com.smartpharma.service.AssistantService;
import com.smartpharma.service.settings.SmartFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    private final AssistantProvider assistantProvider;
    private final SmartFeatureSettingsService smartFeatureSettingsService;

    @Override
    @Transactional(readOnly = true)
    public AssistantAnswer ask(String query, Long pharmacyId) {
        Boolean enabled = smartFeatureSettingsService.getOrCreate(pharmacyId).getAiAssistantEnabled();
        if (enabled != null && !enabled) {
            throw new LocalizedException(HttpStatus.FORBIDDEN, "FEATURE_DISABLED_AI_ASSISTANT",
                    "AI assistant feature is disabled for this pharmacy");
        }
        if (query == null || query.trim().isEmpty()) {
            throw new LocalizedException(HttpStatus.BAD_REQUEST, "ASSISTANT_QUESTION_REQUIRED",
                    "Question cannot be empty");
        }
        return assistantProvider.answer(query, pharmacyId);
    }
}
