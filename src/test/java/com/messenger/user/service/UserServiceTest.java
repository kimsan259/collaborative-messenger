package com.messenger.user.service;

import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import com.messenger.user.dto.UserRegistrationRequest;
import com.messenger.user.dto.UserResponse;
import com.messenger.user.entity.User;
import com.messenger.user.entity.UserRole;
import com.messenger.user.entity.UserStatus;
import com.messenger.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * UserServiceTest - 사용자 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private UserService userService;

    private User createTestUser() {
        return User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .displayName("테스트 사용자")
                .email("test@test.com")
                .status(UserStatus.OFFLINE)
                .role(UserRole.MEMBER)
                .build();
    }

    @Test
    @DisplayName("ID로 사용자 조회 성공")
    void findById_existingUser_returnsUserResponse() {
        User user = createTestUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse result = userService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getDisplayName()).isEqualTo("테스트 사용자");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 BusinessException 발생")
    void findById_nonExistingUser_throwsException() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("회원가입 시 비밀번호가 암호화되어 저장됨")
    void register_encodesPassword() {
        // given
        UserRegistrationRequest request = new UserRegistrationRequest(
                "newuser", "password123", "새 사용자", "new@test.com");

        given(userRepository.existsByUsername("newuser")).willReturn(false);
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("$2a$10$encodedPassword");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        userService.register(request);

        // then
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(emailVerificationService).assertAndConsumeVerifiedEmail("new@test.com");
    }

    @Test
    @DisplayName("중복된 username으로 가입 시도 시 예외 발생")
    void register_duplicateUsername_throwsException() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "existinguser", "password", "이름", "test@test.com");
        given(userRepository.existsByUsername("existinguser")).willReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("중복된 email로 가입 시도 시 예외 발생")
    void register_duplicateEmail_throwsException() {
        UserRegistrationRequest request = new UserRegistrationRequest(
                "newuser", "password", "이름", "existing@test.com");
        given(userRepository.existsByUsername("newuser")).willReturn(false);
        given(userRepository.existsByEmail("existing@test.com")).willReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("프로필 수정 성공")
    void updateProfile_success() {
        User user = createTestUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse result = userService.updateProfile(1L, "새이름", "new@email.com");

        assertThat(result.getDisplayName()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("비밀번호 변경 - 현재 비밀번호 불일치 시 예외 발생")
    void changePassword_wrongCurrentPassword_throwsException() {
        User user = createTestUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPw", "encodedPassword")).willReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, "wrongPw", "newPw"))
                .isInstanceOf(BusinessException.class);
    }
}
