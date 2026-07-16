package com.smartpharma.controller;

import com.smartpharma.security.JwtService;
import com.smartpharma.service.NotificationStreamService;
import com.smartpharma.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationStreamController {

    private final NotificationStreamService notificationStreamService;
    private final SessionService sessionService;
    private final JwtService jwtService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotifications(
            @RequestParam Long pharmacyId,
            @RequestParam String token) {

        if (sessionService.validateSession(token) == null) {
            return ResponseEntity.status(401).build();
        }

        Long tokenPharmacyId = jwtService.extractPharmacyId(token);
        if (tokenPharmacyId != null && !tokenPharmacyId.equals(pharmacyId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(notificationStreamService.subscribe(pharmacyId));
    }
}
