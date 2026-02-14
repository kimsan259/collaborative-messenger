package com.messenger.user.entity;

import com.messenger.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * ============================================================
 * User - 사용자 엔티티
 * ============================================================
 *
 * 【역할】
 * 서비스에 가입한 사용자 한 명의 정보를 나타냅니다.
 * 데이터베이스의 'users' 테이블과 1:1로 매핑됩니다.
 *
 * 【테이블 매핑】
 * 이 클래스의 각 필드가 users 테이블의 컬럼에 대응합니다:
 *   id           → id (PK, 자동 증가)
 *   username     → username (로그인 ID, UNIQUE)
 *   password     → password (BCrypt 해시)
 *   displayName  → display_name
 *   email        → email
 *   status       → status (VARCHAR로 저장: "ONLINE", "OFFLINE", "AWAY")
 *   role         → role (VARCHAR로 저장: "ADMIN", "MANAGER", "MEMBER")
 *   createdAt    → created_at (BaseEntity에서 상속)
 *   updatedAt    → updated_at (BaseEntity에서 상속)
 *
 * 【저장 위치】 Shard 0 (기본 DB)
 * ============================================================
 */
@Entity                          // JPA 엔티티: 이 클래스가 DB 테이블과 매핑됨
@Table(name = "users")           // 매핑될 테이블 이름
@Getter                          // 모든 필드의 getter 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA가 사용하는 기본 생성자 (외부 사용 방지)
@AllArgsConstructor              // 모든 필드를 받는 생성자
@Builder                         // 빌더 패턴으로 객체 생성 가능 (User.builder().username("kim").build())
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // MySQL AUTO_INCREMENT 사용
    private Long id;

    /** 로그인에 사용하는 고유 아이디 (중복 불가) */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /** BCrypt로 암호화된 비밀번호 (평문은 절대 저장하지 않음) */
    @Column(nullable = false)
    private String password;

    /** 채팅방에서 표시되는 이름 (한글 가능) */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /** 이메일 주소 (선택 항목) */
    @Column(length = 100)
    private String email;

    /** 접속 상태: ONLINE, OFFLINE, AWAY */
    @Enumerated(EnumType.STRING)   // enum을 문자열로 저장 (숫자가 아닌 "ONLINE" 형태)
    @Column(length = 20)
    private UserStatus status;

    /** 프로필 이미지 파일 경로 */
    @Column(name = "profile_image", length = 255)
    private String profileImage;

    /** 시스템 권한: ADMIN, MANAGER, MEMBER */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role;

    // ===== 비즈니스 메서드 (상태 변경은 메서드를 통해서만 가능) =====

    /** 사용자의 접속 상태를 변경합니다. */
    public void updateStatus(UserStatus newStatus) {
        this.status = newStatus;
    }

    /** 사용자 정보를 수정합니다. */
    public void updateProfile(String displayName, String email) {
        this.displayName = displayName;
        this.email = email;
    }

    /** 비밀번호를 변경합니다. (이미 암호화된 값을 전달해야 함) */
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 프로필 이미지를 변경합니다. */
    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    /** 사용자의 권한을 변경합니다. (관리자 전용) */
    public void updateRole(UserRole newRole) {
        this.role = newRole;
    }
}
