package com.quickbite.auth.dto;

import com.quickbite.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String provider;
    private String profilePicUrl;
    private boolean isActive;
    private boolean isEmailVerified;
    private LocalDateTime createdAt;
    
    public static UserDto fromEntity(User user) {
        return UserDto.builder()
            .userId(user.getUserId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .role(user.getRole().name())
            .provider(user.getProvider() != null ? user.getProvider().name() : null)
            .profilePicUrl(user.getProfilePicUrl())
            .isActive(user.isActive())
            .isEmailVerified(user.isEmailVerified())
            .createdAt(user.getCreatedAt())
            .build();
    }
}

// ProfileUpdateRequest.java
