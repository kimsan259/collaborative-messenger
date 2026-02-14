package com.messenger.report.repository;

import com.messenger.report.entity.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 * DailyReportRepository - 업무일지 데이터 접근 인터페이스
 * ============================================================
 */
@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

    /** 특정 사용자의 특정 날짜 업무일지 조회 */
    Optional<DailyReport> findByUserIdAndReportDate(Long userId, LocalDate reportDate);

    /** 특정 사용자의 업무일지 목록 조회 (최신순) */
    List<DailyReport> findByUserIdOrderByReportDateDesc(Long userId);

    /** 특정 날짜에 이미 업무일지가 존재하는지 확인 */
    boolean existsByUserIdAndReportDate(Long userId, LocalDate reportDate);
}
