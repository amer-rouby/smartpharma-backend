package com.smartpharma.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    private Long pharmacyId;

    @Size(max = 100)
    private String pharmacyName;

    @Size(max = 50)
    private String licenseNumber;

    @Email(message = "Invalid email address")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6)
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "^01[0-9]{9}$", message = "Invalid Egyptian phone number")
    private String phone;

    // Role is only used when registering to an existing pharmacy (pharmacyId provided)
    // When creating a new pharmacy (pharmacyName provided), the role is forced to ADMIN
    private String role;
}