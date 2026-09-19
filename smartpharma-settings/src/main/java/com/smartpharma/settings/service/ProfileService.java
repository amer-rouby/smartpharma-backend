package com.smartpharma.settings.service;

import com.smartpharma.settings.dto.request.ProfileUpdateRequest;
import com.smartpharma.settings.dto.response.ProfileResponse;

public interface ProfileService {

    ProfileResponse getProfile(Long userId);
    ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request);
    ProfileResponse changePassword(Long userId, String oldPassword, String newPassword);

    void updateProfileImageUrl(Long userId, String imageUrl);
}