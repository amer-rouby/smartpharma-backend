package com.smartpharma.platform.service;

import com.smartpharma.platform.dto.request.CreateShareLinkRequest;
import com.smartpharma.platform.dto.response.ShareLinkResponse;
import com.smartpharma.platform.entity.ShareLink;

public interface ShareLinkService {

    ShareLinkResponse createShareLink(CreateShareLinkRequest request, Long createdBy, Long pharmacyId);

    ShareLink validateShareLink(String token);

    void incrementAccessCount(String token);

    void cleanupExpiredLinks();

    String buildShareUrl(ShareLink shareLink);
}