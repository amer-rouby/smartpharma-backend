package com.smartpharma.dto.request;

import com.smartpharma.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @Email(message = "Invalid email address")
    private String email;

    @Size(max = 20)
    private String phone;

    @NotNull(message = "Role is required")
    private User.UserRole role;

    @NotNull(message = "Pharmacy is required")
    private Long pharmacyId;

    @Builder.Default
    private Boolean isActive = true;
}