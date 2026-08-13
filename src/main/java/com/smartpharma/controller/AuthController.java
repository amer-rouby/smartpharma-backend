package com.smartpharma.controller;

import com.smartpharma.dto.request.LoginRequest;
import com.smartpharma.dto.request.RegisterRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.AuthResponse;
import com.smartpharma.dto.response.ExtendSessionResponse;
import com.smartpharma.dto.response.SessionStatusResponse;
import com.smartpharma.service.AuthenticationService;
import com.smartpharma.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final SessionService sessionService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/2fa/login")
    public ResponseEntity<AuthResponse> completeTwoFactorLogin(@RequestBody Map<String, String> body) {
        AuthResponse response = authenticationService.completeTwoFactorLogin(
                body.get("tempToken"), body.get("code"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authenticationService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        authenticationService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/session-status")
    public ResponseEntity<SessionStatusResponse> getSessionStatus(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        SessionStatusResponse status = sessionService.getSessionStatus(token);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/extend-session")
    public ResponseEntity<ExtendSessionResponse> extendSession(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        ExtendSessionResponse response = sessionService.extendSession(token);
        return ResponseEntity.ok(response);
    }
}