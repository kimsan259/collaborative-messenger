package com.messenger.chat.controller;

import com.messenger.chat.dto.ChatMessageRequest;
import com.messenger.chat.event.ChatMessageEvent;
import com.messenger.infrastructure.kafka.ChatMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * ============================================================
 * ChatWebSocketController - WebSocket 채팅 메시지 핸들러
 * ============================================================
 *
 * 【역할】
 * 클라이언트가 WebSocket(STOMP)으로 보낸 메시지를 받아서 처리합니다.
 * HTTP 요청이 아닌 WebSocket 메시지를 처리하는 컨트롤러입니다.
 *
 * 【@MessageMapping vs @GetMapping/@PostMapping】
 * - @GetMapping, @PostMapping: HTTP 요청을 처리
 * - @MessageMapping: WebSocket STOMP 메시지를 처리
 *
 * 【전체 메시지 흐름 - 가장 중요한 흐름!】
 *
 *  [사용자 브라우저]
 *       │
 *       │ stompClient.publish({ destination: '/app/chat.send', body: {...} })
 *       │
 *       ▼
 *  ★ [ChatWebSocketController.sendMessage()]  ← 여기!
 *       │
 *       │ 1. 메시지 유효성 검증
 *       │ 2. ChatMessageEvent 생성
 *       │ 3. Kafka에 발행
 *       │
 *       ▼
 *  [Kafka: chat.message.sent 토픽]
 *       │
 *       ▼
 *  [ChatMessageConsumer.consumeMessage()]
 *       │
 *       │ 1. DB에 저장 (샤딩)
 *       │ 2. WebSocket 브로드캐스트
 *       │ 3. 멘션 알림 생성
 *       │
 *       ▼
 *  [모든 구독자 브라우저에 메시지 도착]
 *
 * 【왜 여기서 바로 DB에 저장하지 않는가?】
 * Kafka를 거치면:
 * 1. DB 장애 시에도 메시지 유실 없음 (Kafka에 보관)
 * 2. 메시지 순서 보장 (같은 채팅방 = 같은 파티션 = 순서 보장)
 * 3. 트래픽 급증 시에도 안정적 (Kafka가 버퍼 역할)
 * ============================================================
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageProducer chatMessageProducer;

    /**
     * 【채팅 메시지 수신 핸들러】
     *
     * 클라이언트가 /app/chat.send 로 보낸 STOMP 메시지를 처리합니다.
     *
     * @param request        메시지 내용 (chatRoomId, content, messageType)
     * @param headerAccessor WebSocket 세션 정보 (사용자 정보 포함)
     */
    @MessageMapping("/chat.send")  // 클라이언트가 /app/chat.send 로 보내면 이 메서드가 실행됨
    public void sendMessage(
            @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        // ===== 세션에서 사용자 정보 추출 =====
        // WebSocket 연결 시 HTTP 세션이 함께 전달됨
        Long senderId = getUserIdFromSession(headerAccessor);
        String senderName = getUserNameFromSession(headerAccessor);

        log.info("[메시지 수신] 채팅방ID={}, 발신자={}({}), 내용 길이={}자",
                request.getChatRoomId(), senderName, senderId,
                request.getContent() != null ? request.getContent().length() : 0);

        // ===== Kafka 이벤트 생성 =====
        ChatMessageEvent event = ChatMessageEvent.builder()
                .chatRoomId(request.getChatRoomId())
                .senderId(senderId)
                .senderName(senderName)
                .content(request.getContent())
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentName(request.getAttachmentName())
                .attachmentContentType(request.getAttachmentContentType())
                .attachmentSize(request.getAttachmentSize())
                .sentAt(LocalDateTime.now())
                .build();

        // ===== Kafka에 발행 (비동기) =====
        // 이 시점에서 메시지가 Kafka 토픽으로 전달됩니다.
        // ChatMessageConsumer가 이 메시지를 소비하여 DB 저장 + 브로드캐스트를 처리합니다.
        chatMessageProducer.sendMessage(event);
    }

    /**
     * 【타이핑 인디케이터 핸들러】
     * 사용자가 메시지를 입력 중일 때 다른 사용자에게 "입력 중..." 표시
     * (향후 구현: SimpMessagingTemplate으로 브로드캐스트)
     */
    @MessageMapping("/chat.typing")
    public void typing(SimpMessageHeaderAccessor headerAccessor) {
        // TODO: 타이핑 인디케이터 구현
        log.debug("[타이핑] 사용자가 입력 중...");
    }

    /** 세션에서 사용자 ID를 추출합니다. */
    private Long getUserIdFromSession(SimpMessageHeaderAccessor headerAccessor) {
        Object userId = headerAccessor.getSessionAttributes() != null
                ? headerAccessor.getSessionAttributes().get("userId")
                : null;
        return userId != null ? Long.valueOf(userId.toString()) : 0L;
    }

    /** 세션에서 사용자 이름을 추출합니다. */
    private String getUserNameFromSession(SimpMessageHeaderAccessor headerAccessor) {
        Object displayName = headerAccessor.getSessionAttributes() != null
                ? headerAccessor.getSessionAttributes().get("displayName")
                : null;
        return displayName != null ? displayName.toString() : "익명";
    }
}
