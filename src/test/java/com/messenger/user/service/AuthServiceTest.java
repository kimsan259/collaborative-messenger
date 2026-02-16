package com.messenger.user.service;

import com.messenger.common.exception.BusinessException;
import com.messenger.user.dto.LoginRequest;
import com.messenger.user.dto.UserResponse;
import com.messenger.user.entity.User;
import com.messenger.user.entity.UserRole;
import com.messenger.user.entity.UserStatus;
import com.messenger.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * AuthServiceTest - 인증 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AuthService authService;

    private User createActiveUser() {
        return User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .displayName("테스트")
                .email("test@test.com")
                .status(UserStatus.OFFLINE)
                .role(UserRole.MEMBER)
                .active(true)
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("정상 로그인 성공")
    void login_validCredentials_returnsUserResponse() {
        User user = createActiveUser();
        LoginRequest request = new LoginRequest("testuser", "password");

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password", "encodedPassword")).willReturn(true);
        given(session.getId()).willReturn("session-id");

        UserResponse result = authService.login(request, session);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(session).setAttribute("userId", 1L);
        verify(session).setAttribute("username", "testuser");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 로그인 시 예외 발생")
    void login_nonExistingUser_throwsException() {
        LoginRequest request = new LoginRequest("unknown", "password");
        given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, session))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("비밀번호 불일치 시 예외 발생")
    void login_wrongPassword_throwsException() {
        User user = createActiveUser();
        LoginRequest request = new LoginRequest("testuser", "wrongPassword");

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request, session))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("비활성화된 계정 로그인 시 예외 발생")
    void login_inactiveUser_throwsException() {
        User user = User.builder()
                .id(1L).username("testuser").password("encodedPassword")
                .displayName("테스트").email("test@test.com")
                .status(UserStatus.OFFLINE).role(UserRole.MEMBER)
                .active(false).emailVerified(true)
                .build();
        LoginRequest request = new LoginRequest("testuser", "password");

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password", "encodedPassword")).willReturn(true);

        assertThatThrownBy(() -> authService.login(request, session))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("이메일 미인증 계정 로그인 시 예외 발생 (일반 사용자)")
    void login_emailNotVerified_throwsException() {
        User user = User.builder()
                .id(1L).username("testuser").password("encodedPassword")
                .displayName("테스트").email("test@test.com")
                .status(UserStatus.OFFLINE).role(UserRole.MEMBER)
                .active(true).emailVerified(false)
                .build();
        LoginRequest request = new LoginRequest("testuser", "password");

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password", "encodedPassword")).willReturn(true);

        assertThatThrownBy(() -> authService.login(request, session))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("로그아웃 시 사용자 상태를 OFFLINE으로 변경")
    void logout_setsUserOffline() {
        User user = createActiveUser();
        user.updateStatus(UserStatus.ONLINE);

        given(session.getAttribute("userId")).willReturn(1L);
        given(session.getAttribute("username")).willReturn("testuser");
        given(session.getId()).willReturn("session-id");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        authService.logout(session);

        assertThat(user.getStatus()).isEqualTo(UserStatus.OFFLINE);
        verify(session).invalidate();
    }
}
