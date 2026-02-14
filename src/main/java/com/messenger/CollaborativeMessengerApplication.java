package com.messenger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * ============================================================
 * CollaborativeMessengerApplication - 애플리케이션 진입점
 * ============================================================
 *
 * 【역할】
 * Spring Boot 애플리케이션의 시작점(main 메서드)입니다.
 * 이 클래스를 실행하면 내장 톰캣 서버가 시작되고, 모든 설정이 자동으로 로딩됩니다.
 *
 * 【어노테이션 설명】
 * - @SpringBootApplication: Spring Boot의 자동 설정, 컴포넌트 스캔, 설정 클래스를 한번에 활성화
 * - @EnableJpaAuditing: BaseEntity의 @CreatedDate, @LastModifiedDate가 자동으로 동작하게 함
 *
 * 【참고: @EnableAsync, @EnableScheduling은 별도 설정 클래스에 있습니다】
 * - AsyncConfig.java → @EnableAsync (비동기 처리)
 * - SchedulingConfig.java → @EnableScheduling (스케줄링)
 * 각 설정을 별도 클래스로 분리한 이유: 관련 설정을 한 곳에서 관리하기 위함
 * ============================================================
 */
@SpringBootApplication    // Spring Boot 핵심 어노테이션 (자동 설정 + 컴포넌트 스캔)
@EnableJpaAuditing        // JPA 감사(Auditing) 기능 활성화 (createdAt, updatedAt 자동 기록)
public class CollaborativeMessengerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollaborativeMessengerApplication.class, args);
    }

}
