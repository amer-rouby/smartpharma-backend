package com.smartpharma.controller;

import com.smartpharma.dto.request.AssistantQuestionRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.AssistantAnswer;
import com.smartpharma.service.AssistantService;
import com.smartpharma.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// No try/catch - a disabled feature flag or empty question throws
// LocalizedException, handled globally with the correct status/error code.
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@Slf4j
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/ask")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AssistantAnswer>> ask(@Valid @RequestBody AssistantQuestionRequest request) {
        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();
        AssistantAnswer answer = assistantService.ask(request.getQuery(), pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(answer));
    }
}
