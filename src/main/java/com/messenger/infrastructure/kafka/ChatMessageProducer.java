package com.messenger.infrastructure.kafka;

import com.messenger.chat.event.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;

/**
 * ============================================================
 * ChatMessageProducer - Kafka 메시지 발행자 (Producer)
 * ============================================================
 *
 * 【역할】
 * 채팅 메시지를 Kafka 토픽("chat.message.sent")에 발행합니다.
 *
 * 【전체 흐름에서의 위치】
 * [사용자] → [WebSocket] → [ChatWebSocketController]
 *     → ★ [ChatMessageProducer] → [Kafka 토픽]
 *     → [ChatMessageConsumer] → [DB 저장 + 브로드캐스트]
 *
 * 【직렬화 전략】
 * ChatMessageEvent → JSON 문자열 → Kafka (String 직렬화)
 * Jackson 3.x의 ObjectMapper를 사용하여 직접 JSON 변환합니다.
 * ============================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageProducer {

    /** Kafka에 문자열 메시지를 보내는 핵심 객체 */
    private final KafkaTemplate<String, String> kafkaTemplate;

    /** JSON 변환용 ObjectMapper (Jackson 3.x) */
    private final ObjectMapper objectMapper;

    /** Kafka 토픽 이름 (상수로 관리하여 오타 방지) */
    private static final String TOPIC = "chat.message.sent";

    /**
     * 【채팅 메시지를 Kafka에 발행】
     *
     * @param event 발행할 메시지 이벤트
     *
     * 동작:
     * 1. ChatMessageEvent를 JSON 문자열로 변환
     * 2. kafkaTemplate.send(토픽명, 키, JSON문자열) 호출
     * 3. 키 = chatRoomId (문자열로 변환) → 파티션 라우팅에 사용
     * 4. 발행 결과를 CompletableFuture로 비동기 처리
     */
    public void sendMessage(ChatMessageEvent event) {
        try {
            // 파티션 키: chatRoomId → 같은 채팅방의 메시지 순서 보장
            String partitionKey = String.valueOf(event.getChatRoomId());

            // ChatMessageEvent → JSON 문자열 변환
            String jsonMessage = objectMapper.writeValueAsString(event);

            log.debug("[Kafka 발행] 토픽={}, 파티션키={}, 발신자={}, 채팅방ID={}",
                    TOPIC, partitionKey, event.getSenderName(), event.getChatRoomId());

            // Kafka에 비동기로 메시지 전송
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(TOPIC, partitionKey, jsonMessage);

            // 전송 결과를 비동기로 처리 (성공/실패 콜백)
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("[Kafka 발행 실패] 채팅방ID={}, 에러={}",
                            event.getChatRoomId(), throwable.getMessage());
                } else {
                    log.debug("[Kafka 발행 성공] 채팅방ID={}, 파티션={}, 오프셋={}",
                            event.getChatRoomId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("[Kafka 발행 실패] JSON 변환 에러 - 채팅방ID={}, 에러={}",
                    event.getChatRoomId(), e.getMessage(), e);
        }
    }
}
