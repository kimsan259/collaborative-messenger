package com.messenger.infrastructure.kafka;

import com.messenger.chat.dto.ChatMessageResponse;
import com.messenger.chat.entity.ChatMessage;
import com.messenger.chat.entity.MessageType;
import com.messenger.chat.event.ChatMessageEvent;
import com.messenger.chat.repository.ChatMessageRepository;
import com.messenger.infrastructure.sharding.ShardKeyHolder;
import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

/**
 * ============================================================
 * ChatMessageConsumer - Kafka 메시지 소비자 (Consumer)
 * ============================================================
 *
 * 【역할】
 * Kafka 토픽("chat.message.sent")에서 JSON 문자열을 가져와 처리합니다.
 * ★ 이 클래스는 시스템의 핵심 허브로, 가장 많은 역할을 합니다.
 *
 * 【전체 흐름에서의 위치 - 가장 중요한 클래스!】
 * [Kafka 토픽: chat.message.sent]
 *     → ★ [ChatMessageConsumer.consumeMessage()]
 *         1. JSON 문자열 → ChatMessageEvent 역직렬화
 *         2. DB에 메시지 저장 (MySQL 샤딩 라우팅 포함)
 *         3. WebSocket으로 채팅방 구독자에게 브로드캐스트
 *         4. 멘션(@)이 있으면 알림 생성
 *
 * 【왜 Kafka → DB 순서인가?】
 * 1. 장애 격리: DB가 잠깐 죽어도 Kafka에 메시지가 보관되어 유실 없음
 * 2. 순서 보장: Kafka 파티션 내 메시지는 순서대로 처리됨
 * 3. 부하 분산: 트래픽이 많을 때 Consumer 수를 늘려 병렬 처리 가능
 * ============================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;  // WebSocket 메시지 전송 도구
    private final ObjectMapper objectMapper;  // JSON 역직렬화용 (Jackson 3.x)

    /**
     * 【Kafka 메시지 소비 핸들러 - 시스템의 심장】
     *
     * Kafka 토픽에서 JSON 문자열을 받아 처리하는 메인 메서드입니다.
     *
     * @param jsonMessage Kafka에서 수신한 JSON 문자열
     */
    @KafkaListener(
            topics = "chat.message.sent",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMessage(String jsonMessage) {
        try {
            // ===== 1단계: JSON 문자열 → ChatMessageEvent 역직렬화 =====
            ChatMessageEvent event = objectMapper.readValue(jsonMessage, ChatMessageEvent.class);

            log.debug("[Kafka 소비] 채팅방ID={}, 발신자={}, 내용 미리보기={}",
                    event.getChatRoomId(),
                    event.getSenderName(),
                    event.getContent() != null && event.getContent().length() > 20
                            ? event.getContent().substring(0, 20) + "..."
                            : event.getContent());

            // ===== 2단계: ChatMessage 엔티티 생성 =====
            ChatMessage chatMessage = ChatMessage.builder()
                    .chatRoomId(event.getChatRoomId())
                    .senderId(event.getSenderId())
                    .content(event.getContent())
                    .messageType(parseMessageType(event.getMessageType()))
                    .attachmentUrl(event.getAttachmentUrl())
                    .attachmentName(event.getAttachmentName())
                    .attachmentContentType(event.getAttachmentContentType())
                    .attachmentSize(event.getAttachmentSize())
                    .mentions(event.getMentions())
                    .sentAt(event.getSentAt() != null ? event.getSentAt() : LocalDateTime.now())
                    .build();

            // ===== 3단계: DB에 저장 (★ 샤딩 라우팅 포함) =====
            // ShardKeyHolder에 chatRoomId를 설정하여 올바른 MySQL 샤드에 저장
            ChatMessage savedMessage;
            try {
                ShardKeyHolder.set(event.getChatRoomId());
                savedMessage = chatMessageRepository.save(chatMessage);
            } finally {
                ShardKeyHolder.clear();  // ★ 반드시 해제 (메모리 누수 방지)
            }
            log.debug("[DB 저장 완료] 메시지ID={}, 채팅방ID={}, 샤드=shard_{}",
                    savedMessage.getId(), savedMessage.getChatRoomId(),
                    event.getChatRoomId() % 2);

            // ===== 4단계: 응답 DTO 생성 (프로필 이미지 포함) =====
            String senderProfileImage = null;
            try {
                ShardKeyHolder.set(0L);
                senderProfileImage = userRepository.findById(event.getSenderId())
                        .map(User::getProfileImage).orElse(null);
            } finally {
                ShardKeyHolder.clear();
            }
            ChatMessageResponse response = ChatMessageResponse.from(
                    savedMessage, event.getSenderName(), senderProfileImage, 0);

            // ===== 5단계: WebSocket으로 채팅방 구독자에게 브로드캐스트 =====
            String destination = "/topic/chatroom/" + event.getChatRoomId();
            messagingTemplate.convertAndSend(destination, response);
            log.debug("[WebSocket 브로드캐스트] 목적지={}", destination);

            // ===== 6단계: 멘션 알림 처리 =====
            if (event.getMentions() != null && !event.getMentions().isEmpty()) {
                log.debug("[멘션 감지] 멘션 대상: {}", event.getMentions());
            }

        } catch (Exception e) {
            log.error("[Kafka 소비 실패] JSON={}, 에러={}", jsonMessage, e.getMessage(), e);
        }
    }

    /**
     * 문자열을 MessageType enum으로 변환합니다.
     * 잘못된 값이 들어오면 기본값 TEXT를 반환합니다.
     */
    private MessageType parseMessageType(String type) {
        try {
            return type != null ? MessageType.valueOf(type) : MessageType.TEXT;
        } catch (IllegalArgumentException e) {
            return MessageType.TEXT;
        }
    }
}
