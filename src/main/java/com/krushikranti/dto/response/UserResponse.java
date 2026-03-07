package com.krushikranti.dto.response;

import com.krushikranti.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String profileImageUrl;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public static UserResponse fromEntity(User user) {
        String status = "active";
        if (!user.isEnabled()) {
            status = "banned";
        } else if (!user.isVerified()) {
            status = "pending";
        }

        // Map role enum to frontend format (remove ROLE_ prefix and lowercase)
        String roleStr = user.getRole().name().replace("ROLE_", "").toLowerCase();

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .role(roleStr)
                .status(status)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getUpdatedAt()) // Using updatedAt as proxy for lastLogin
                .build();
    }
}
