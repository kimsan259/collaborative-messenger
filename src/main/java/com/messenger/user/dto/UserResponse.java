package com.messenger.user.dto;

import com.messenger.user.entity.User;
import com.messenger.user.entity.UserRole;
import com.messenger.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String displayName;
    private String email;
    private String githubUsername;
    private String profileImage;
    private UserStatus status;
    private UserRole role;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .githubUsername(user.getGithubUsername())
                .profileImage(user.getProfileImage())
                .status(user.getStatus())
                .role(user.getRole())
                .build();
    }
}
