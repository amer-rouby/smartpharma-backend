package com.smartpharma.service.impl;

import com.smartpharma.dto.request.CreateShareLinkRequest;
import com.smartpharma.dto.response.ShareLinkResponse;
import com.smartpharma.entity.ShareLink;
import com.smartpharma.repository.ShareLinkRepository;
import com.smartpharma.service.ShareLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareLinkServiceImpl implements ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;

    @Value("${app.base-url:https://smartpharma.app}")
    private String baseUrl;

    @Override
    @Transactional
    public ShareLinkResponse createShareLink(CreateShareLinkRequest request, Long createdBy, Long pharmacyId) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(request.getExpiryHours());

        ShareLink shareLink = ShareLink.builder()
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .pharmacyId(pharmacyId)
                .expiresAt(expiresAt)
                .createdBy(createdBy)
                .isActive(true)
                .accessCount(0)
                .build();

        shareLink.generateToken();

        ShareLink saved = shareLinkRepository.save(shareLink);

        String shareUrl = String.format("%s/share/%s/%s",
                baseUrl,
                saved.getEntityType().toLowerCase(),
                saved.getToken());

        log.info("Share link created: token={}, entityType={}, entityId={}, expiresAt={}",
                saved.getToken(), saved.getEntityType(), saved.getEntityId(), saved.getExpiresAt());

        return ShareLinkResponse.builder()
                .shareUrl(shareUrl)
                .token(saved.getToken())
                .expiresAt(saved.getExpiresAt())
                .entityType(saved.getEntityType())
                .entityId(saved.getEntityId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ShareLink validateShareLink(String token) {
        return shareLinkRepository.findActiveByToken(token, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired share link"));
    }

    @Override
    @Transactional
    public void incrementAccessCount(String token) {
        shareLinkRepository.incrementAccessCount(token);
    }

    // تنظيف الروابط منتهية الصلاحية كل ساعة
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredLinks() {
        shareLinkRepository.deactivateExpiredLinks(LocalDateTime.now());
        log.info("Expired share links cleaned up");
    }
}