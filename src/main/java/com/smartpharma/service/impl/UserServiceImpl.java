package com.smartpharma.service.impl;

import com.smartpharma.dto.request.UserRequest;
import com.smartpharma.dto.response.UserResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.User;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(Long pharmacyId) {
        return userRepository.findByPharmacyId(pharmacyId)
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id, Long pharmacyId) {
        User user = userRepository.findByIdAndPharmacyId(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email exists
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(request.getRole())
                .pharmacy(pharmacy)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        User saved = userRepository.save(user);
        log.info("User created: {} for pharmacy {}", saved.getUsername(), pharmacy.getId());
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request, Long pharmacyId) {
        User user = userRepository.findByIdAndPharmacyId(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if new username conflicts
        if (!user.getUsername().equals(request.getUsername()) &&
                userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if new email conflicts
        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : user.getIsActive());

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updated = userRepository.save(user);
        log.info("User updated: {} for pharmacy {}", updated.getUsername(), pharmacyId);
        return UserResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, Long pharmacyId) {
        User user = userRepository.findByIdAndPharmacyId(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Soft delete
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("User deleted (soft): {} for pharmacy {}", user.getUsername(), pharmacyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(Long pharmacyId, String query) {
        return userRepository.searchByPharmacyIdAndUsername(pharmacyId, query)
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUsersCount(Long pharmacyId) {
        return userRepository.countByPharmacyId(pharmacyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getActiveUsers(Long pharmacyId) {
        return userRepository.findActiveByPharmacyId(pharmacyId)
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }
}