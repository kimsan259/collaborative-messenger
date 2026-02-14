package com.messenger.user.repository;

import com.messenger.user.entity.User;
import com.messenger.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 * UserRepository - 사용자 데이터 접근 인터페이스
 * ============================================================
 *
 * 【역할】
 * users 테이블에 대한 CRUD(생성, 조회, 수정, 삭제) 작업을 담당합니다.
 *
 * 【Spring Data JPA 사용법】
 * JpaRepository를 상속하면 기본 메서드가 자동 제공됩니다:
 *   - save(entity): 저장 (INSERT 또는 UPDATE)
 *   - findById(id): ID로 조회
 *   - findAll(): 전체 조회
 *   - deleteById(id): ID로 삭제
 *   - count(): 전체 개수
 *
 * 【메서드 이름 규칙으로 쿼리 자동 생성】
 * 메서드 이름에 따라 Spring이 자동으로 SQL을 만들어줍니다:
 *   findByUsername("kim") → SELECT * FROM users WHERE username = 'kim'
 *   existsByUsername("kim") → SELECT COUNT(*) > 0 FROM users WHERE username = 'kim'
 * ============================================================
 */
@Repository  // Spring Bean으로 등록 (데이터 접근 계층임을 명시)
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 로그인 ID(username)로 사용자를 조회합니다.
     * 로그인 시 사용됩니다.
     *
     * @param username 로그인 ID
     * @return Optional<User> - 사용자가 있으면 값이 들어있고, 없으면 비어있음
     */
    Optional<User> findByUsername(String username);

    /**
     * 해당 username이 이미 존재하는지 확인합니다.
     * 회원가입 시 중복 체크에 사용됩니다.
     *
     * @param username 확인할 로그인 ID
     * @return true면 이미 존재, false면 사용 가능
     */
    boolean existsByUsername(String username);

    /**
     * 해당 email이 이미 존재하는지 확인합니다.
     *
     * @param email 확인할 이메일
     * @return true면 이미 존재, false면 사용 가능
     */
    boolean existsByEmail(String email);

    /**
     * 특정 접속 상태의 사용자 목록을 조회합니다.
     * 예: 현재 ONLINE인 사용자 목록
     *
     * @param status 조회할 상태 (ONLINE, OFFLINE, AWAY)
     * @return 해당 상태의 사용자 목록
     */
    List<User> findByStatus(UserStatus status);

    /** username 또는 displayName에 키워드가 포함된 사용자 검색 */
    List<User> findByUsernameContainingOrDisplayNameContaining(String username, String displayName);
}
