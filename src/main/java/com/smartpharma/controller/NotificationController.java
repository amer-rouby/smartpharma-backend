package com.smartpharma.controller;

import com.smartpharma.dto.request.NotificationRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.NotificationResponse;
import com.smartpharma.security.JwtService;
import com.smartpharma.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    private Long extractUserIdFromToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                Long userId = jwtService.extractUserId(jwt);
                if (userId != null) {
                    return userId;
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract userId from token", e);
        }
        return null;
    }

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

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid token"));
        }

        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

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

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid token"));
        }

        int count = notificationService.markAllAsRead(pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(count, "All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid token"));
        }

        notificationService.deleteNotification(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification deleted"));
    }

    @PostMapping("/check-alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")  // ✅ فقط admins و managers يقدروا يشغلوا الـ alerts
    public ResponseEntity<ApiResponse<Void>> checkAndCreateAlerts(@RequestParam Long pharmacyId) {
        notificationService.checkAndCreateLowStockAlerts(pharmacyId);
        notificationService.checkAndCreateExpiryAlerts(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "Alerts checked successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @RequestBody NotificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        NotificationResponse notification = notificationService.createNotification(request);
        return ResponseEntity.ok(ApiResponse.success(notification, "Notification created"));
    }
}