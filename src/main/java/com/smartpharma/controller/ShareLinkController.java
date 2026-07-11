package com.smartpharma.controller;

import com.smartpharma.dto.request.CreateShareLinkRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.ShareLinkResponse;
import com.smartpharma.entity.ShareLink;
import com.smartpharma.service.ShareLinkService;
import com.smartpharma.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
@Slf4j
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> createShareLink(
            @Valid @RequestBody CreateShareLinkRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = SecurityUtils.extractUserId(userDetails);
        Long pharmacyId = SecurityUtils.extractPharmacyId(userDetails);

        log.info("Creating share link for entityType: {}, entityId: {}",
                request.getEntityType(), request.getEntityId());

        ShareLinkResponse response = shareLinkService.createShareLink(
                request, userId, pharmacyId);

        return ResponseEntity.ok(ApiResponse.success(response, "Share link generated"));
    }

    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> getSharedData(
            @PathVariable String token) {

        log.info("Accessing shared data with token: {}", token);

        ShareLink shareLink = shareLinkService.validateShareLink(token);
        shareLinkService.incrementAccessCount(token);
        return ResponseEntity.ok(ApiResponse.success(
                ShareLinkResponse.builder()
                        .shareUrl("")
                        .token(token)
                        .expiresAt(shareLink.getExpiresAt())
                        .entityType(shareLink.getEntityType())
                        .entityId(shareLink.getEntityId())
                        .build(),
                "Shared data retrieved successfully"));
    }
}
