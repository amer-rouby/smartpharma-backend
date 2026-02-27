package com.smartpharma.service.impl.settings;

import com.smartpharma.dto.settings.request.ProfileUpdateRequest;
import com.smartpharma.dto.settings.response.ProfileResponse;
import com.smartpharma.entity.User;
import com.smartpharma.entity.settings.UserProfile;
import com.smartpharma.repository.settings.UserProfileRepository;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.service.settings.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        return buildProfileResponse(user, profile);
    }

    @Override @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean userChanged = false;
        if (request.getFullName() != null && !request.getFullName().isBlank()
                && !request.getFullName().equals(user.getFullName())) {
            user.setFullName(request.getFullName()); userChanged = true;
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail()); userChanged = true;
        }
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            user.setPhone(request.getPhone()); userChanged = true;
        }
        if (userChanged) userRepository.save(user);

        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = UserProfile.builder().user(user).build();
                    return profileRepository.save(newProfile);
                });

        boolean profileChanged = false;
        if (request.getProfileImageUrl() != null && !request.getProfileImageUrl().equals(profile.getProfileImageUrl())) {
            profile.setProfileImageUrl(request.getProfileImageUrl()); profileChanged = true;
        }
        if (request.getJobTitle() != null && !request.getJobTitle().equals(profile.getJobTitle())) {
            profile.setJobTitle(request.getJobTitle()); profileChanged = true;
        }
        if (request.getDepartment() != null && !request.getDepartment().equals(profile.getDepartment())) {
            profile.setDepartment(request.getDepartment()); profileChanged = true;
        }
        if (request.getAddress() != null && !request.getAddress().equals(profile.getAddress())) {
            profile.setAddress(request.getAddress()); profileChanged = true;
        }
        if (request.getCity() != null && !request.getCity().equals(profile.getCity())) {
            profile.setCity(request.getCity()); profileChanged = true;
        }
        if (request.getCountry() != null && !request.getCountry().equals(profile.getCountry())) {
            profile.setCountry(request.getCountry()); profileChanged = true;
        }
        if (request.getBio() != null && !request.getBio().equals(profile.getBio())) {
            profile.setBio(request.getBio()); profileChanged = true;
        }
        if (request.getGender() != null && !request.getGender().equals(profile.getGender())) {
            profile.setGender(request.getGender()); profileChanged = true;
        }
        if (profileChanged) profileRepository.save(profile);

        log.info("Profile updated for userId: {}", userId);
        return buildProfileResponse(user, profile);
    }

    @Override @Transactional
    public ProfileResponse changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for userId: {}", userId);
        return getProfile(userId);
    }

    // ✅ NEW: Update profile image URL
    @Override @Transactional
    public void updateProfileImageUrl(Long userId, String imageUrl) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    UserProfile newProfile = UserProfile.builder().user(user).build();
                    return profileRepository.save(newProfile);
                });
        profile.setProfileImageUrl(imageUrl);
        profileRepository.save(profile);
        log.info("Profile image updated for userId: {}", userId);
    }

    private ProfileResponse buildProfileResponse(User user, UserProfile profile) {
        return ProfileResponse.builder()
                .userId(user.getId()).username(user.getUsername()).fullName(user.getFullName())
                .email(user.getEmail()).phone(user.getPhone())
                .profileImageUrl(profile != null ? profile.getProfileImageUrl() : null)
                .jobTitle(profile != null ? profile.getJobTitle() : null)
                .department(profile != null ? profile.getDepartment() : null)
                .address(profile != null ? profile.getAddress() : null)
                .city(profile != null ? profile.getCity() : null)
                .country(profile != null ? profile.getCountry() : null)
                .bio(profile != null ? profile.getBio() : null)
                .gender(profile != null ? profile.getGender() : null)
                .role(user.getRole()).lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
    }
}