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
 * ============================================================
 * UserServiceTest - 사용자 서비스 단위 테스트
 * ============================================================
 *
 * 【테스트 방식: 단위 테스트 (Mockito)】
 * - UserRepository를 Mock으로 대체하여 DB 없이 테스트
 * - PasswordEncoder를 Mock으로 대체하여 실제 BCrypt 없이 테스트
 * - 순수하게 UserService의 비즈니스 로직만 검증
 *
 * 【왜 Mock을 사용하는가?】
 * - 단위 테스트는 하나의 클래스만 격리하여 테스트해야 합니다
 * - DB 연결 없이 빠르게 실행 가능 (밀리초 단위)
 * - 실패 시 UserService 로직의 문제인지 명확히 알 수 있음
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // ===== 테스트 데이터 헬퍼 =====

    private UserRegistrationRequest createRegistrationRequest() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        // 리플렉션 대신 적절한 테스트 요청 생성
        return request;
    }

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

    // ===== 테스트 케이스 =====

    @Test
    @DisplayName("ID로 사용자 조회 성공")
    void findById_existingUser_returnsUserResponse() {
        // given: DB에 사용자가 존재하는 상황
        User user = createTestUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        UserResponse result = userService.findById(1L);

        // then: 올바른 응답이 반환되는지 확인
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getDisplayName()).isEqualTo("테스트 사용자");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 → BusinessException 발생")
    void findById_nonExistingUser_throwsException() {
        // given: DB에 해당 ID의 사용자가 없는 상황
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then: USER_NOT_FOUND 예외가 발생해야 함
        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("회원가입 시 비밀번호가 BCrypt로 암호화됨")
    void register_encodesPassword() {
        // given
        given(userRepository.existsByUsername(anyString())).willReturn(false);
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("$2a$10$encodedPassword");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            return saved;
        });

        // when
        userService.register("newuser", "password123", "새 사용자", "new@test.com");

        // then: PasswordEncoder.encode()가 호출되었는지 확인
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("중복된 username으로 가입 시도 → BusinessException 발생")
    void register_duplicateUsername_throwsException() {
        // given: 이미 같은 username이 존재하는 상황
        given(userRepository.existsByUsername("existinguser")).willReturn(true);

        // when & then: DUPLICATE_USERNAME 예외가 발생해야 함
        assertThatThrownBy(() ->
                userService.register("existinguser", "password", "이름", "test@test.com"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("중복된 email로 가입 시도 → BusinessException 발생")
    void register_duplicateEmail_throwsException() {
        // given: username은 중복 아니지만 email이 중복인 상황
        given(userRepository.existsByUsername("newuser")).willReturn(false);
        given(userRepository.existsByEmail("existing@test.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                userService.register("newuser", "password", "이름", "existing@test.com"))
                .isInstanceOf(BusinessException.class);
    }
}
