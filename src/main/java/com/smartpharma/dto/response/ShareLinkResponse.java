package com.smartpharma.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLinkResponse {
    private String shareUrl;
    private String token;
    private LocalDateTime expiresAt;
    private String entityType;
    private Long entityId;
    /** The actual shared entity payload (e.g. a DemandPredictionResponse), null if this
     * entityType isn't wired up for sharing yet. */
    private Object data;
}