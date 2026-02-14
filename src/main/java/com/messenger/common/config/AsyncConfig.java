package com.messenger.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * ============================================================
 * AsyncConfig - 비동기 처리 스레드풀 설정
 * ============================================================
 *
 * 【역할】
 * @Async 어노테이션이 붙은 메서드를 실행할 때 사용하는 스레드풀을 설정합니다.
 *
 * 【왜 별도의 스레드풀이 필요한가?】
 * 기본적으로 Spring의 @Async는 매번 새로운 스레드를 생성합니다.
 * 이렇게 하면 스레드가 무한정 생성될 수 있어 서버가 다운될 수 있습니다.
 * 스레드풀을 사용하면 최대 스레드 수를 제한하여 안전하게 비동기 처리를 할 수 있습니다.
 *
 * 【스레드풀 개념 (수영장 비유)】
 * - corePoolSize(2):  수영장에 항상 대기하는 안전요원 수 (평소에 유지)
 * - maxPoolSize(5):   수영장이 만원일 때 추가로 부를 수 있는 최대 안전요원 수
 * - queueCapacity(100): 안전요원이 모두 바쁠 때 대기줄에 설 수 있는 최대 인원
 *
 * 【이 프로젝트의 스레드풀 구성】
 * 1. reportGenerationExecutor: 업무일지 생성 전용 (2~5 스레드)
 * 2. notificationExecutor: 알림 발송 전용 (3~10 스레드)
 *
 * 분리하는 이유: 업무일지 생성이 오래 걸려도 알림 발송에 영향을 주지 않음
 * ============================================================
 */
@Slf4j
@Configuration       // Spring 설정 클래스임을 선언
@EnableAsync         // @Async 어노테이션 활성화
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 【업무일지 생성 전용 스레드풀】
     *
     * 매일 퇴근 시간(18:00)에 모든 사용자의 업무일지를 동시에 생성합니다.
     * 이 작업은 DB 조회가 많아서 시간이 걸리므로, 전용 스레드풀에서 처리합니다.
     *
     * 사용처: ReportGenerationService.generateReport()
     * 사용법: @Async("reportGenerationExecutor")
     *
     * 로그에서 이 스레드풀의 스레드는 "report-gen-1", "report-gen-2" 등으로 표시됩니다.
     */
    @Bean("reportGenerationExecutor")
    public Executor reportGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);          // 기본 스레드 2개 유지
        executor.setMaxPoolSize(5);           // 최대 5개까지 확장
        executor.setQueueCapacity(100);       // 대기 큐 100개
        executor.setThreadNamePrefix("report-gen-");   // 로그에서 식별 가능한 접두사
        executor.setWaitForTasksToCompleteOnShutdown(true);  // 서버 종료 시 작업 완료까지 대기
        executor.setAwaitTerminationSeconds(60);  // 최대 60초 대기
        executor.initialize();

        log.info("[AsyncConfig] 업무일지 생성 스레드풀 초기화 완료 (core={}, max={}, queue={})",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }

    /**
     * 【알림 발송 전용 스레드풀】
     *
     * 멘션(@)이 발생하거나, 업무일지가 생성되면 알림을 비동기로 발송합니다.
     * 알림은 빠르게 처리되어야 하므로 스레드 수를 넉넉하게 설정합니다.
     *
     * 사용처: NotificationService.sendNotification()
     * 사용법: @Async("notificationExecutor")
     *
     * 로그에서 이 스레드풀의 스레드는 "notification-1", "notification-2" 등으로 표시됩니다.
     */
    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);          // 기본 스레드 3개 유지
        executor.setMaxPoolSize(10);          // 최대 10개까지 확장
        executor.setQueueCapacity(500);       // 대기 큐 500개 (알림이 몰릴 수 있으므로 넉넉하게)
        executor.setThreadNamePrefix("notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("[AsyncConfig] 알림 발송 스레드풀 초기화 완료 (core={}, max={}, queue={})",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }

    /**
     * 【비동기 실행 중 예외 처리】
     *
     * @Async 메서드에서 예외가 발생하면 호출자(caller)에게 전달되지 않습니다.
     * (비동기이므로 호출자는 이미 다른 일을 하고 있음)
     * 이 핸들러에서 예외를 로그로 기록하여 문제를 놓치지 않도록 합니다.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable throwable, Method method, Object... params) -> {
            log.error("[비동기 에러] 메서드={}, 파라미터={}, 에러={}",
                    method.getName(), params, throwable.getMessage(), throwable);
        };
    }
}
