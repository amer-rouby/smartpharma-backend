package com.smartpharma.service;

import com.smartpharma.dto.response.AssistantAnswer;

// Swap-point for a future real LLM-backed implementation, per the "0-cost for
// now" decision. RuleBasedAssistantProvider is the only implementation today -
// nothing else about the controller/service contract needs to change to add
// a real one later.
public interface AssistantProvider {
    AssistantAnswer answer(String query, Long pharmacyId);
}
