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

/**
 * ============================================================
 * UserService - 사용자 비즈니스 로직
 * ============================================================
 *
 * 【역할】
 * 사용자 관련 핵심 비즈니스 로직을 처리합니다.
 *   - 회원가입 (중복 체크 + 비밀번호 암호화 + 저장)
 *   - 사용자 조회 (ID/username으로)
 *   - 사용자 목록 조회
 *   - 프로필 수정
 *
 * 【계층 구조에서의 위치】
 * Controller → Service → Repository → Database
 *   - Controller: HTTP 요청 받기 + 응답 반환 (얇은 레이어)
 *   - Service:    비즈니스 로직 처리 (핵심 레이어) ← 여기
 *   - Repository: 데이터베이스 접근 (데이터 레이어)
 *
 * 【@Transactional 설명】
 * - 클래스 레벨에 readOnly = true: 기본적으로 모든 메서드가 읽기 전용 트랜잭션
 * - 쓰기가 필요한 메서드에만 @Transactional 추가 (readOnly 해제)
 * - 이렇게 하면 읽기 메서드에서 실수로 데이터를 변경하는 것을 방지할 수 있음
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor    // final 필드를 주입받는 생성자를 자동 생성 (생성자 주입)
@Transactional(readOnly = true)  // 기본: 읽기 전용 트랜잭션 (성능 최적화)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;  // Spring Security의 비밀번호 암호화 도구

    /**
     * 【회원가입】
     *
     * 처리 순서:
     * 1. 아이디 중복 체크 → 중복이면 BusinessException 발생
     * 2. 이메일 중복 체크 → 중복이면 BusinessException 발생
     * 3. 비밀번호를 BCrypt로 암호화 (평문을 DB에 저장하면 안 됨!)
     * 4. User 엔티티 생성 후 DB에 저장
     * 5. 저장된 사용자 정보를 UserResponse로 변환하여 반환
     *
     * @param request 회원가입 요청 데이터 (username, password, displayName, email)
     * @return 생성된 사용자 정보 (비밀번호 제외)
     */
    @Transactional  // 쓰기 작업이므로 readOnly 해제
    public UserResponse register(UserRegistrationRequest request) {
        log.info("[회원가입] 아이디={} 으로 회원가입 시도", request.getUsername());

        // 1단계: 아이디 중복 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // 2단계: 이메일 중복 체크 (이메일이 입력된 경우에만)
        if (request.getEmail() != null && !request.getEmail().isEmpty()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 3단계: 비밀번호 암호화
        // BCrypt: 같은 비밀번호라도 매번 다른 해시값이 생성됨 (레인보우 테이블 공격 방지)
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 4단계: User 엔티티 생성 (Builder 패턴 사용)
        User user = User.builder()
                .username(request.getUsername())
                .password(encodedPassword)           // ★ 암호화된 비밀번호 저장
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                .status(UserStatus.OFFLINE)           // 가입 직후에는 오프라인 상태
                .role(UserRole.MEMBER)                // 기본 권한은 일반 사용자
                .build();

        // 5단계: DB에 저장
        User savedUser = userRepository.save(user);
        log.info("[회원가입] 완료 - 사용자ID={}, 아이디={}", savedUser.getId(), savedUser.getUsername());

        // 6단계: 엔티티 → DTO 변환 후 반환
        return UserResponse.from(savedUser);
    }

    /**
     * 【ID로 사용자 조회】
     *
     * @param userId 사용자 고유 번호
     * @return 사용자 정보 (없으면 BusinessException 발생)
     */
    public UserResponse findById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    /**
     * 【username으로 사용자 조회 (내부용)】
     * Service 내부에서 User 엔티티가 필요할 때 사용합니다.
     */
    public User findEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 【전체 사용자 목록 조회】
     */
    public List<UserResponse> findAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)    // 각 User 엔티티를 UserResponse로 변환
                .collect(Collectors.toList());
    }

    /**
     * 【사용자 프로필 수정】
     */
    @Transactional
    public UserResponse updateProfile(Long userId, String displayName, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateProfile(displayName, email);

        log.info("[프로필 수정] 사용자ID={}, 이름={}", userId, displayName);
        return UserResponse.from(user);
    }

    /**
     * 【비밀번호 변경】
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
        log.info("[비밀번호 변경] 사용자ID={}", userId);
    }

    /**
     * 【프로필 이미지 변경】
     */
    @Transactional
    public void updateProfileImage(Long userId, String profileImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateProfileImage(profileImage);
        log.info("[프로필 이미지 변경] 사용자ID={}", userId);
    }
}
