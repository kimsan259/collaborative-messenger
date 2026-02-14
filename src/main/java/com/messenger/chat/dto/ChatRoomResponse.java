package com.messenger.chat.dto;

import com.messenger.chat.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * ============================================================
 * ChatRoomResponse - 채팅방 정보 응답 DTO
 * ============================================================
 */
@Getter
@Builder
@AllArgsConstructor
public class ChatRoomResponse {

    private Long id;
    private String name;
    private String roomType;
    private int memberCount;
    private long unreadCount;       // 읽지 않은 메시지 수
    private String lastMessage;     // 마지막 메시지 미리보기
    private String lastMessageTime; // 마지막 메시지 시간

    /** ChatRoom 엔티티를 응답 DTO로 변환합니다. */
    public static ChatRoomResponse from(ChatRoom chatRoom, int memberCount) {
        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .roomType(chatRoom.getRoomType().name())
                .memberCount(memberCount)
                .unreadCount(0)
                .build();
    }

    /** unreadCount와 lastMessage를 포함한 변환 */
    public static ChatRoomResponse from(ChatRoom chatRoom, int memberCount, long unreadCount,
                                         String lastMessage, String lastMessageTime) {
        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .roomType(chatRoom.getRoomType().name())
                .memberCount(memberCount)
                .unreadCount(unreadCount)
                .lastMessage(lastMessage)
                .lastMessageTime(lastMessageTime)
                .build();
    }
}
