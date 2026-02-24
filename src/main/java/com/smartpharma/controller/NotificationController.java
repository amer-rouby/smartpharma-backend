package com.smartpharma.controller;

import com.smartpharma.dto.request.NotificationRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.NotificationResponse;
import com.smartpharma.security.JwtService;  // ✅ أضف الـ import ده
import com.smartpharma.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("isAuthenticated()")  // ✅ أي user مسجل دخول يقدر يدخل
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;  // ✅ أضف الـ dependency ده

    // ✅ Helper method: استخرج userId من الـ token مش من الـ username
    private Long extractUserIdFromToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                Long userId = jwtService.extractUserId(jwt);  // ← ده اللي هيفلحنا!
                if (userId != null) {
                    return userId;
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract userId from token", e);
        }
        return null;
    }

    // ================================
    // ✅ GET /api/notifications
    // ================================
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getUserNotifications(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid token"));
        }

        Page<NotificationResponse> notifications = notificationService.getUserNotifications(pharmacyId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    // ================================
    // ✅ GET /api/notifications/unread-count  ← اللي أنت بتجربه
    // ================================
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid token"));
        }

        Long count = notificationService.getUnreadCount(pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // ================================
    // ✅ GET /api/notifications/unread  ← اللي أنت بتجربه كمان
    // ================================
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<java.util.List<NotificationResponse>>> getUnreadNotifications(
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid token"));
        }

        var notifications = notificationService.getUnreadNotifications(pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    // ================================
    // ✅ PUT /api/notifications/{id}/read
    // ================================
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid token"));
        }

        NotificationResponse notification = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success(notification, "Notification marked as read"));
    }

    // ================================
    // ✅ POST /api/notifications/check-alerts
    // ================================
    @PostMapping("/check-alerts")
    public ResponseEntity<ApiResponse<Void>> checkAndCreateAlerts(@RequestParam Long pharmacyId) {
        notificationService.checkAndCreateLowStockAlerts(pharmacyId);
        notificationService.checkAndCreateExpiryAlerts(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Alerts checked successfully"));
    }
}