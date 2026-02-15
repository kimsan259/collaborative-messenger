package com.messenger.user.entity;

import com.messenger.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus status;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    public void updateStatus(UserStatus newStatus) {
        this.status = newStatus;
    }

    public void updateProfile(String displayName, String email) {
        this.displayName = displayName;
        this.email = email;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updateRole(UserRole newRole) {
        this.role = newRole;
    }

    public void deactivate() {
        this.active = false;
        this.status = UserStatus.OFFLINE;
    }

    public void activate() {
        this.active = true;
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }
}
