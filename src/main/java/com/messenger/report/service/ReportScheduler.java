package com.messenger.report.service;

import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * ============================================================
 * ReportScheduler - 업무일지 자동 생성 스케줄러
 * ============================================================
 *
 * 【역할】
 * 매일 월~금 오후 6시에 모든 사용자의 업무일지를 자동 생성합니다.
 *
 * 【동작 흐름】
 * 1. @Scheduled(cron)에 의해 정해진 시각에 triggerDailyReports()가 자동 호출
 * 2. 모든 사용자 목록을 조회
 * 3. 각 사용자에 대해 ReportGenerationService.generateReportAsync() 호출
 *    (★ @Async이므로 각 사용자별 생성이 병렬로 실행됨)
 * 4. 사용자 50명이면 → 5개 스레드(maxPoolSize=5)가 동시에 처리
 *
 * 【cron 표현식 읽는 법】
 *   "초 분 시 일 월 요일"
 *   "0  0  18 *  *  MON-FRI"
 *    │  │  │  │  │  └── 요일: 월~금
 *    │  │  │  │  └── 월: 매월
 *    │  │  │  └── 일: 매일
 *    │  │  └── 시: 18시
 *    │  └── 분: 0분
 *    └── 초: 0초
 *   = 매주 월~금 18:00:00에 실행
 *
 * 【테스트 팁】
 * application.yml에서 cron 값을 변경하여 테스트할 수 있습니다:
 *   - 매 1분마다: "0 * * * * *"
 *   - 매 30초마다: "0/30 * * * * *"
 *   - 비활성화: report.generation.enabled=false
 * ============================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduler {

    private final UserRepository userRepository;
    private final ReportGenerationService reportGenerationService;

    /** 업무일지 생성 활성화 여부 (application.yml에서 설정) */
    @Value("${report.generation.enabled:true}")
    private boolean enabled;

    /**
     * 【업무일지 자동 생성 트리거】
     *
     * @Scheduled: Spring이 정해진 시각에 자동으로 이 메서드를 호출합니다.
     * cron 값은 application.yml의 report.generation.cron에서 가져옵니다.
     *
     * ★ 이 메서드는 Spring 스케줄러 스레드에서 실행됩니다.
     *   실제 업무일지 생성은 @Async 스레드풀에서 병렬로 실행됩니다.
     */
    @Scheduled(cron = "${report.generation.cron}")
    public void triggerDailyReports() {
        // 비활성화 상태면 실행하지 않음
        if (!enabled) {
            log.info("[업무일지 스케줄러] 비활성화 상태 - 스킵");
            return;
        }

        LocalDate today = LocalDate.now();
        log.info("====================================================");
        log.info("[업무일지 스케줄러] 자동 생성 시작 - 날짜={}", today);
        log.info("====================================================");

        // 모든 사용자 조회
        List<User> allUsers = userRepository.findAll();
        log.info("[업무일지 스케줄러] 대상 사용자 수={}", allUsers.size());

        // 각 사용자에 대해 비동기로 업무일지 생성
        for (User user : allUsers) {
            log.debug("[업무일지 스케줄러] 업무일지 생성 요청 - 사용자={}", user.getUsername());
            reportGenerationService.generateReportAsync(user, today);
            // ★ @Async이므로 여기서 리턴됨 (생성 완료를 기다리지 않음)
        }

        log.info("[업무일지 스케줄러] 모든 사용자에 대해 생성 요청 완료 (비동기 실행 중)");
    }
}
