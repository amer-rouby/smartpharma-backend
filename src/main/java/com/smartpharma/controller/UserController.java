package com.smartpharma.controller;

import com.smartpharma.dto.request.UserRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.UserResponse;
import com.smartpharma.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam Long pharmacyId) {

        log.info("GET /api/users - pharmacyId: {}", pharmacyId);

        List<UserResponse> users = userService.getAllUsers(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUsersCount(
            @RequestParam Long pharmacyId) {

        log.info("GET /api/users/count - pharmacyId: {}", pharmacyId);

        Long count = userService.getUsersCount(pharmacyId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(ApiResponse.success(response, "Users count retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        log.info("GET /api/users/{} - pharmacyId: {}", id, pharmacyId);

        UserResponse user = userService.getUser(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserRequest request) {

        log.info("POST /api/users - pharmacyId: {}, username: {}",
                request.getPharmacyId(), request.getUsername());

        UserResponse user = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success(user, "User created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request,
            @RequestParam Long pharmacyId) {

        log.info("PUT /api/users/{} - pharmacyId: {}", id, pharmacyId);

        UserResponse user = userService.updateUser(id, request, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(user, "User updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {

        log.info("DELETE /api/users/{} - pharmacyId: {}", id, pharmacyId);

        userService.deleteUser(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam Long pharmacyId,
            @RequestParam String query) {

        log.info("GET /api/users/search - pharmacyId: {}, query: '{}'", pharmacyId, query);

        List<UserResponse> users = userService.searchUsers(pharmacyId, query);
        return ResponseEntity.ok(ApiResponse.success(users, "Search completed successfully"));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getActiveUsers(
            @RequestParam Long pharmacyId) {

        log.info("GET /api/users/active - pharmacyId: {}", pharmacyId);

        List<UserResponse> users = userService.getActiveUsers(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(users, "Active users retrieved successfully"));
    }
}