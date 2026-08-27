package com.smartpharma.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssistantQuestionRequest {

    @NotBlank(message = "Question is required")
    private String query;
}
