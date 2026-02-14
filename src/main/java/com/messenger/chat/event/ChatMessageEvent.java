package com.messenger.chat.event;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ============================================================
 * ChatMessageEvent - Kafka를 통해 전달되는 채팅 메시지 이벤트
 * ============================================================
 *
 * 【역할】
 * WebSocket으로 들어온 채팅 메시지를 Kafka 토픽에 발행할 때 사용하는 이벤트 객체입니다.
 *
 * 【전체 흐름에서의 위치】
 * 1. 사용자가 메시지 전송 (WebSocket)
 * 2. ChatWebSocketController가 이 이벤트 객체를 생성
 * 3. ChatMessageProducer가 Kafka 토픽에 이 이벤트를 발행(publish)
 * 4. ChatMessageConsumer가 이 이벤트를 소비(consume)하여 DB 저장 + WebSocket 브로드캐스트
 *
 * 【왜 Entity가 아닌 별도 Event 객체를 쓰는가?】
 * 1. Entity는 JPA 관리 대상이라 직렬화 시 문제가 생길 수 있음
 * 2. Kafka 메시지에는 DB의 ID가 아직 없음 (저장 전이므로)
 * 3. 필요한 필드만 담아서 네트워크 전송 효율화
 *
 * 【Serializable】
 * Kafka로 전송하려면 직렬화(객체 → 바이트 변환)가 가능해야 합니다.
 * ============================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 메시지를 보낼 채팅방 ID (★ Kafka 파티션 키로도 사용) */
    private Long chatRoomId;

    /** 메시지를 보낸 사용자 ID */
    private Long senderId;

    /** 메시지를 보낸 사용자 표시 이름 */
    private String senderName;

    /** 메시지 내용 */
    private String content;

    /** 메시지 유형: TEXT, IMAGE, FILE, SYSTEM */
    private String messageType;

    private String attachmentUrl;
    private String attachmentName;
    private String attachmentContentType;
    private Long attachmentSize;

    /** 멘션된 사용자 ID 목록 (쉼표로 구분) */
    private String mentions;

    /** 메시지 발송 시각 */
    private LocalDateTime sentAt;
}
