package com.krushikranti.dto.request;

import com.krushikranti.model.User;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
    private String password;

    @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Invalid Indian phone number")
    private String phone;

    @NotNull(message = "Role is required")
    private User.Role role;

    @AssertFalse(message = "Admin registration is not allowed through this endpoint")
    private boolean isAdmin() {
        return role == User.Role.ROLE_ADMIN;
    }
}
