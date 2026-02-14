package com.messenger.chat.entity;

import com.messenger.common.entity.BaseEntity;
import com.messenger.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ============================================================
 * ChatRoomMember - 채팅방 참여자 엔티티
 * ============================================================
 *
 * 【역할】
 * 어떤 사용자가 어떤 채팅방에 참여하고 있는지를 관리합니다.
 * "읽지 않은 메시지" 수를 계산하기 위한 lastReadAt 필드를 포함합니다.
 *
 * 【테이블】 chat_room_members (Shard 0에 저장)
 * ============================================================
 */
@Entity
@Table(name = "chat_room_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoomMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 참여한 채팅방 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    /** 참여자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * 마지막으로 읽은 시각.
     * 이 시각 이후에 도착한 메시지 수 = "읽지 않은 메시지" 수
     */
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    /** 메시지를 읽었을 때 호출하여 lastReadAt을 갱신합니다. */
    public void markAsRead() {
        this.lastReadAt = LocalDateTime.now();
    }
}
