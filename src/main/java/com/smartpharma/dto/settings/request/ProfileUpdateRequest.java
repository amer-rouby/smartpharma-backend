package com.smartpharma.dto.settings.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {

    @Size(max = 100)
    private String fullName;

    @Email(message = "البريد الإلكتروني غير صحيح")
    @Size(max = 100)
    private String email;

    @Size(max = 50)
    private String phone;

    @Size(max = 255)
    private String profileImageUrl;

    @Size(max = 100)
    private String jobTitle;

    @Size(max = 50)
    private String department;

    @Size(max = 100)
    private String address;

    @Size(max = 50)
    private String city;

    @Size(max = 50)
    private String country;

    @Size(max = 500)
    private String bio;

    private String gender;
}