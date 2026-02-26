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

    @NotBlank(message = "اسم المستخدم مطلوب")
    @Size(min = 3, max = 50, message = "اسم المستخدم يجب أن يكون بين 3 و 50 حرف")
    private String username;

    @Size(min = 6, message = "كلمة المرور يجب أن تكون 6 أحرف على الأقل")
    private String password;

    @NotBlank(message = "الاسم الكامل مطلوب")
    @Size(max = 100)
    private String fullName;

    @Email(message = "البريد الإلكتروني غير صحيح")
    private String email;

    @Size(max = 20)
    private String phone;

    @NotNull(message = "الدور مطلوب")
    private User.UserRole role;

    @NotNull(message = "الصيدلية مطلوبة")
    private Long pharmacyId;

    @Builder.Default
    private Boolean isActive = true;
}