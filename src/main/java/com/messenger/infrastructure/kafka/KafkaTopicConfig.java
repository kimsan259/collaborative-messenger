package com.messenger.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * ============================================================
 * KafkaTopicConfig - Kafka 토픽 자동 생성 설정
 * ============================================================
 *
 * 【역할】
 * 애플리케이션이 시작될 때 필요한 Kafka 토픽을 자동으로 생성합니다.
 * docker-compose에서 KAFKA_AUTO_CREATE_TOPICS_ENABLE=false로 설정했으므로,
 * 여기서 명시적으로 토픽을 생성합니다.
 *
 * 【토픽 구성】
 * 1. chat.message.sent (파티션 4개): 채팅 메시지 처리용
 * 2. chat.notification (파티션 2개): 알림 발생 이벤트용
 * 3. report.generation.trigger (파티션 1개): 업무일지 생성 트리거
 *
 * 【파티션 수 결정 기준】
 * - 파티션 수 = 최대 병렬 Consumer 수
 * - 채팅 메시지: 트래픽이 많으므로 4개 (4개 Consumer가 동시 처리 가능)
 * - 알림: 중간 정도 트래픽이므로 2개
 * - 업무일지: 하루 1번 실행이므로 1개면 충분
 * ============================================================
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * 【채팅 메시지 토픽】
     * 파티션 키: chatRoomId (같은 채팅방의 메시지는 같은 파티션으로 → 순서 보장)
     * replicas: 1 (개발 환경이므로 복제 없음, 운영에서는 3 권장)
     */
    @Bean
    public NewTopic chatMessageTopic() {
        return TopicBuilder.name("chat.message.sent")
                .partitions(4)
                .replicas(1)
                .build();
    }

    /**
     * 【알림 이벤트 토픽】
     * 멘션(@) 발생, 채팅방 초대 등의 알림 이벤트를 전달합니다.
     */
    @Bean
    public NewTopic chatNotificationTopic() {
        return TopicBuilder.name("chat.notification")
                .partitions(2)
                .replicas(1)
                .build();
    }

    /**
     * 【업무일지 생성 트리거 토픽】
     * @Scheduled가 발행하여 업무일지 생성을 트리거합니다.
     */
    @Bean
    public NewTopic reportGenerationTopic() {
        return TopicBuilder.name("report.generation.trigger")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
