package com.smartpharma.controller.settings;

import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.settings.request.ProfileUpdateRequest;
import com.smartpharma.dto.settings.response.ProfileResponse;
import com.smartpharma.entity.User;
import com.smartpharma.service.settings.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class ProfileController {

    private final ProfileService profileService;

    @Value("${app.upload-dir:uploads/profiles/}")
    private String uploadDir;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @RequestParam Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long authenticatedUserId = ((User) userDetails).getId();
        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized"));
        }

        log.info("GET /api/profile - userId: {}", userId);
        ProfileResponse profile = profileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved successfully"));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @RequestParam Long userId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request) {

        Long authenticatedUserId = ((User) userDetails).getId();
        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized"));
        }

        log.info("PUT /api/profile - userId: {}", userId);
        ProfileResponse profile = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile updated successfully"));
    }
    @PostMapping("/upload-image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId;
        if (userDetails instanceof User user) {
            userId = user.getId();
        } else {
            return ResponseEntity.status(403).body(ApiResponse.error("Invalid authentication"));
        }

        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body(ApiResponse.error("No file selected"));

            String contentType = file.getContentType();
            List<String> allowedTypes = List.of("image/png", "image/jpeg", "image/jpg", "image/webp");
            if (contentType == null || !allowedTypes.contains(contentType)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid file type. Allowed: PNG, JPEG, WEBP"));
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File size must be less than 5MB"));
            }

            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID() + extension;

            Path uploadPath = Paths.get(uploadDir).normalize();
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(filename).normalize();
            if (!filePath.startsWith(uploadPath)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid filename"));
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String publicUrl = "/api/profile/images/" + filename;

            profileService.updateProfileImageUrl(userId, publicUrl);

            return ResponseEntity.ok(ApiResponse.success(Map.of("url", publicUrl, "filename", filename), "Image uploaded successfully"));

        } catch (IOException e) {
            log.error("Error uploading profile image for userId: {}", userId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Upload failed: " + e.getMessage()));
        }
    }
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir + filename);
            Resource resource = new FileSystemResource(filePath);
            if (resource.exists()) {
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> changePassword(
            @RequestParam Long userId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> passwordData) {

        Long authenticatedUserId = ((User) userDetails).getId();
        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized"));
        }

        log.info("POST /api/profile/change-password - userId: {}", userId);
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Old password and new password are required"));
        }
        ProfileResponse profile = profileService.changePassword(userId, oldPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success(profile, "Password changed successfully"));
    }
}