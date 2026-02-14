package com.messenger.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ============================================================
 * SchedulingConfig - 스케줄링 활성화 설정
 * ============================================================
 *
 * 【역할】
 * @Scheduled 어노테이션을 사용하여 특정 시각에 자동으로 실행되는 작업을
 * 가능하게 합니다.
 *
 * 【이 프로젝트에서의 사용처】
 * - ReportScheduler: 매일 월~금 오후 6시에 업무일지를 자동 생성
 *   @Scheduled(cron = "0 0 18 * * MON-FRI")
 *
 * 【cron 표현식 읽는 법】
 *   "초 분 시 일 월 요일"
 *   "0  0  18 *  *  MON-FRI" = 매주 월~금 18시 0분 0초에 실행
 *
 * 【주의사항】
 * @EnableScheduling이 없으면 @Scheduled 어노테이션이 동작하지 않습니다.
 * ============================================================
 */
@Configuration
@EnableScheduling    // @Scheduled 어노테이션 활성화
public class SchedulingConfig {
    // 이 클래스는 @EnableScheduling을 활성화하는 것이 목적이므로
    // 별도의 메서드가 필요 없습니다.
}
