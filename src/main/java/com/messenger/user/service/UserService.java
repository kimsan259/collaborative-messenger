package com.messenger.user.service;

import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import com.messenger.user.dto.UserRegistrationRequest;
import com.messenger.user.dto.UserResponse;
import com.messenger.user.entity.User;
import com.messenger.user.entity.UserRole;
import com.messenger.user.entity.UserStatus;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        log.info("[register] username={}", request.getUsername());

        String normalizedEmail = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        if (normalizedEmail.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        emailVerificationService.assertAndConsumeVerifiedEmail(normalizedEmail);

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .displayName(request.getDisplayName())
                .email(normalizedEmail)
                .status(UserStatus.OFFLINE)
                .role(UserRole.MEMBER)
                .active(true)
                .emailVerified(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("[register] completed userId={}, username={}", savedUser.getId(), savedUser.getUsername());
        return UserResponse.from(savedUser);
    }

    public UserResponse findById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    public User findEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public List<UserResponse> findAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateProfile(Long userId, String displayName, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateProfile(displayName, email);
        log.info("[profile-update] userId={}, displayName={}", userId, displayName);
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
        log.info("[password-change] userId={}", userId);
    }

    @Transactional
    public void updateProfileImage(Long userId, String profileImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateProfileImage(profileImage);
        log.info("[profile-image-update] userId={}", userId);
    }
}

