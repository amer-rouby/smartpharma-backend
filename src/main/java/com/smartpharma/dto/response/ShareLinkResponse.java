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
}