package com.smartpharma.service.impl;

import com.smartpharma.dto.request.LoginRequest;
import com.smartpharma.dto.request.RegisterRequest;
import com.smartpharma.dto.response.AuthResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.User;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.security.JwtService;
import com.smartpharma.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Pharmacy pharmacy;

        if (request.getPharmacyName() != null && !request.getPharmacyName().isBlank()) {
            // Create new pharmacy with registration
            if (request.getLicenseNumber() == null || request.getLicenseNumber().isBlank()) {
                throw new RuntimeException("License number is required for pharmacy registration");
            }

            pharmacy = Pharmacy.builder()
                    .name(request.getPharmacyName())
                    .licenseNumber(request.getLicenseNumber())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .subscriptionStatus(Pharmacy.SubscriptionStatus.ACTIVE)
                    .planType(Pharmacy.PlanType.BASIC)
                    .build();

            pharmacy = pharmacyRepository.save(pharmacy);
        } else if (request.getPharmacyId() != null) {
            // Register user to existing pharmacy
            pharmacy = pharmacyRepository.findById(request.getPharmacyId())
                    .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
        } else {
            throw new RuntimeException("Either pharmacyId or pharmacyName is required for registration");
        }

        if (userRepository.existsByPharmacyIdAndUsername(pharmacy.getId(), request.getUsername())) {
            throw new RuntimeException("Username already exists in this pharmacy");
        }

        // Determine role: new pharmacy registration forces ADMIN, existing pharmacy uses provided role or defaults to PHARMACIST
        String roleName;
        if (request.getPharmacyName() != null && !request.getPharmacyName().isBlank()) {
            // First user of a new pharmacy is always ADMIN
            roleName = "ADMIN";
        } else {
            // Existing pharmacy: use provided role or default to PHARMACIST
            roleName = (request.getRole() != null && !request.getRole().isBlank()) ? request.getRole() : "PHARMACIST";
        }

        User user = User.builder()
                .pharmacy(pharmacy)
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(User.UserRole.valueOf(roleName))
                .isActive(true)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user, pharmacy.getId());
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .pharmacyId(pharmacy.getId())
                .pharmacyName(pharmacy.getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.extractClaim(accessToken, claims -> claims.getExpiration().getTime()))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.getIsActive()) {
            throw new RuntimeException("User account is deactivated");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user, user.getPharmacy().getId());
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .pharmacyId(user.getPharmacy().getId())
                .pharmacyName(user.getPharmacy().getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(System.currentTimeMillis() + jwtService.extractClaim(accessToken, claims ->
                        claims.getExpiration().getTime()))
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateToken(user, user.getPharmacy().getId());

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(System.currentTimeMillis() + jwtService.extractClaim(newAccessToken, claims ->
                        claims.getExpiration().getTime()))
                .build();
    }
}