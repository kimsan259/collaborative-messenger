package com.messenger.user.repository;

import com.messenger.user.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ============================================================
 * TeamRepository - 팀 데이터 접근 인터페이스
 * ============================================================
 *
 * 【역할】
 * teams 테이블에 대한 CRUD 작업을 담당합니다.
 * JpaRepository를 상속하여 기본 메서드(save, findById, findAll 등)를 자동 제공받습니다.
 * ============================================================
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    /** 팀 이름이 이미 존재하는지 확인합니다. */
    boolean existsByName(String name);
}
