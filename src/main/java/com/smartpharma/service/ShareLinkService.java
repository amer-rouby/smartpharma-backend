package com.smartpharma.service;

import com.smartpharma.dto.request.CreateShareLinkRequest;
import com.smartpharma.dto.response.ShareLinkResponse;
import com.smartpharma.entity.ShareLink;

public interface ShareLinkService {

    ShareLinkResponse createShareLink(CreateShareLinkRequest request, Long createdBy, Long pharmacyId);

    ShareLink validateShareLink(String token);

    void incrementAccessCount(String token);

    void cleanupExpiredLinks();
}