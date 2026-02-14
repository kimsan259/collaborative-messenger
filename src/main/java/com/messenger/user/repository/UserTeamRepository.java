package com.messenger.user.repository;

import com.messenger.user.entity.UserTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ============================================================
 * UserTeamRepository - 사용자-팀 매핑 데이터 접근 인터페이스
 * ============================================================
 *
 * 【역할】
 * user_teams 테이블에 대한 CRUD 작업을 담당합니다.
 * "어떤 사용자가 어떤 팀에 속하는지" 조회/관리합니다.
 * ============================================================
 */
@Repository
public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {

    /** 특정 사용자가 속한 모든 팀 매핑 조회 */
    List<UserTeam> findByUserId(Long userId);

    /** 특정 팀에 속한 모든 사용자 매핑 조회 */
    List<UserTeam> findByTeamId(Long teamId);

    /** 특정 사용자가 특정 팀에 이미 속해있는지 확인 */
    boolean existsByUserIdAndTeamId(Long userId, Long teamId);
}
