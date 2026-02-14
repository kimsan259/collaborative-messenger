package com.messenger.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * ============================================================
 * JacksonConfig - JSON 직렬화/역직렬화 설정
 * ============================================================
 *
 * 【역할】
 * Java 객체 ↔ JSON 변환 설정을 담당합니다.
 *
 * 【Spring Boot 4 + Jackson 3.x】
 * Spring Boot 4는 Jackson 3.x를 자동 설정합니다:
 * - JavaTimeModule이 Jackson 3.x에 내장되어 별도 등록 불필요
 * - application.yml의 spring.jackson.* 설정으로 제어 가능
 * - 날짜 포맷은 application.yml에서 설정:
 *     spring.jackson.serialization.write-dates-as-timestamps: false
 *
 * 【적용 범위】
 * - REST API의 JSON 응답/요청
 * - Kafka 메시지의 직렬화/역직렬화
 * ============================================================
 */
@Configuration
public class JacksonConfig {
    // Spring Boot 4 + Jackson 3.x: 자동 설정으로 충분합니다.
    // 커스텀 ObjectMapper Bean이 필요하면 여기에 추가합니다.
}
