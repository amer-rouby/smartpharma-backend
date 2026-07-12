package com.smartpharma.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusResponse {
    @JsonProperty("isActive")
    private boolean isActive;
    private String expiresAt;
    private long remainingMinutes;
    private int warningThreshold;
    private boolean canExtend;
    private int remainingExtensions;
}
