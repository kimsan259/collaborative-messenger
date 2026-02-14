package com.messenger.chat.entity;

import com.messenger.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * ============================================================
 * ChatRoom - 채팅방 엔티티
 * ============================================================
 *
 * 【역할】
 * 채팅방 하나의 정보를 나타냅니다.
 * 1:1 채팅방과 그룹 채팅방 모두 이 테이블에 저장됩니다.
 *
 * 【테이블】 chat_rooms (Shard 0에 저장 - 샤딩 대상 아님)
 *
 * 【연관 관계】
 * - ChatRoom 1 : N ChatRoomMember (채팅방에 여러 멤버가 참여)
 * - ChatRoom 1 : N ChatMessage (채팅방에 여러 메시지가 존재)
 * ============================================================
 */
@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 채팅방 이름 (예: "프론트엔드 개발팀 회의") */
    @Column(nullable = false, length = 200)
    private String name;

    /** 채팅방 유형: DIRECT(1:1), GROUP(그룹) */
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private RoomType roomType;

    /** 마지막 메시지 ID (채팅방 목록에서 미리보기용) */
    @Column(name = "last_message_id")
    private Long lastMessageId;

    /** 마지막 메시지 ID를 업데이트합니다. (새 메시지가 저장될 때 호출) */
    public void updateLastMessageId(Long messageId) {
        this.lastMessageId = messageId;
    }
}
