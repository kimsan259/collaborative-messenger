package com.messenger.chat.entity;

import com.messenger.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ============================================================
 * ChatMessage - 채팅 메시지 엔티티 (★ 샤딩 대상)
 * ============================================================
 *
 * 【역할】
 * 채팅방에서 주고받는 메시지 하나를 나타냅니다.
 * 이 테이블은 데이터 양이 가장 많으므로 2개의 MySQL에 분산 저장(샤딩)됩니다.
 *
 * 【샤딩 규칙】
 * chatRoomId % 2 == 0 → Shard 0 (MySQL 포트 3307)에 저장
 * chatRoomId % 2 == 1 → Shard 1 (MySQL 포트 3308)에 저장
 *
 * 예시: chatRoomId=4 → 4%2=0 → Shard 0
 *       chatRoomId=7 → 7%2=1 → Shard 1
 *
 * 【왜 chatRoomId를 샤드 키로 사용하는가?】
 * 같은 채팅방의 메시지가 항상 같은 DB에 저장되므로,
 * 채팅 히스토리 조회 시 하나의 DB만 조회하면 됩니다 (cross-shard 조회 불필요).
 *
 * 【FK를 걸지 않는 이유】
 * users, chat_rooms 테이블이 Shard 0에만 있고 Shard 1에는 없으므로,
 * FK 참조가 불가능합니다. 대신 애플리케이션 코드에서 정합성을 보장합니다.
 *
 * 【테이블】 chat_messages (Shard 0과 Shard 1에 동일한 스키마로 존재)
 * ============================================================
 */
@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ★ 샤드 키: 이 값으로 어떤 MySQL 인스턴스에 저장할지 결정합니다.
     * FK를 걸지 않으므로 @ManyToOne 대신 단순 컬럼으로 관리합니다.
     */
    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    /** 메시지를 보낸 사용자의 ID (FK 대신 값으로 저장) */
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    /** 메시지 내용 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 메시지 유형: TEXT, IMAGE, FILE, SYSTEM */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 20)
    private MessageType messageType;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "attachment_content_type", length = 100)
    private String attachmentContentType;

    @Column(name = "attachment_size")
    private Long attachmentSize;

    /**
     * 멘션된 사용자 ID 목록 (쉼표로 구분)
     * 예: "1,5,12" → 사용자 1, 5, 12가 멘션됨
     */
    @Column(length = 500)
    private String mentions;

    /** 메시지 발송 시각 */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
