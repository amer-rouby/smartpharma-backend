package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLinkDTO {
    private String shareUrl;
    private LocalDateTime expiresAt;
    private String token;
}