package com.smartpharma.controller.settings;

import com.smartpharma.dto.settings.request.ProfileUpdateRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.settings.response.ProfileResponse;
import com.smartpharma.service.settings.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @RequestParam Long userId) {

        log.info("GET /api/profile - userId: {}", userId);

        ProfileResponse profile = profileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved successfully"));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @RequestParam Long userId,
            @Valid @RequestBody ProfileUpdateRequest request) {

        log.info("PUT /api/profile - userId: {}", userId);

        ProfileResponse profile = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile updated successfully"));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> changePassword(
            @RequestParam Long userId,
            @RequestBody Map<String, String> passwordData) {

        log.info("POST /api/profile/change-password - userId: {}", userId);

        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Old password and new password are required")
            );
        }

        ProfileResponse profile = profileService.changePassword(userId, oldPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success(profile, "Password changed successfully"));
    }
}