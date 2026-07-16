package com.smartpharma.controller;

import com.smartpharma.dto.request.UserSettingsRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.UserSettingsResponse;
import com.smartpharma.entity.User;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserSettingsController {

    private final SessionService sessionService;
    private final UserRepository userRepository;

    @GetMapping("/settings")
    public ResponseEntity<UserSettingsResponse> getSettings(@AuthenticationPrincipal User user) {
        // Fetch fresh user data from database
        User freshUser = userRepository.findById(user.getId()).orElse(user);
        int userTimeout = freshUser.getSessionTimeout() != null ? freshUser.getSessionTimeout() : 30;
        
        // Get current session to return expiresAt
        var currentSession = sessionService.getCurrentSession(freshUser.getId());
        LocalDateTime expiresAt = currentSession != null 
                ? currentSession.getExpiresAt() 
                : LocalDateTime.now().plusMinutes(userTimeout);
        
        return ResponseEntity.ok(UserSettingsResponse.builder()
                .sessionTimeout(userTimeout)
                .allowedTimeouts(sessionService.getAllowedTimeouts())
                .expiresAt(expiresAt)
                .build());
    }

    @PutMapping("/settings")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSettings(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserSettingsRequest request) {

        // Validate that the submitted timeout is one of the allowed values
        if (!sessionService.getAllowedTimeouts().contains(request.getSessionTimeout())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Invalid timeout value. Allowed values: " + sessionService.getAllowedTimeouts())
                            .statusCode(400)
                            .build());
        }

        // Fetch fresh user from database and save the timeout preference
        User freshUser = userRepository.findById(user.getId()).orElse(user);
        freshUser.setSessionTimeout(request.getSessionTimeout());
        userRepository.save(freshUser);

        // Extend the current session with the new timeout in real-time
        LocalDateTime newExpiresAt = LocalDateTime.now().plusMinutes(request.getSessionTimeout());
        
        // Update the current session with the new timeout
        sessionService.updateSessionTimeout(freshUser.getId(), request.getSessionTimeout(), newExpiresAt);

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Settings updated successfully")
                .data(Map.of(
                        "success", true,
                        "expiresAt", newExpiresAt.toString(),
                        "sessionTimeout", request.getSessionTimeout()
                ))
                .statusCode(200)
                .build());
    }
}