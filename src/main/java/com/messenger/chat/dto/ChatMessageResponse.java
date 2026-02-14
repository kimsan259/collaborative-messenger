package com.messenger.chat.dto;

import com.messenger.chat.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ============================================================
 * ChatMessageResponse - 채팅 메시지 응답 DTO
 * ============================================================
 *
 * 【역할】
 * WebSocket을 통해 클라이언트에게 메시지를 전달할 때 사용합니다.
 * 발신자 이름(senderName)을 포함하여 별도 조회 없이 바로 표시할 수 있습니다.
 * ============================================================
 */
@Getter
@Builder
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String senderName;
    private String senderProfileImage;
    private String content;
    private String messageType;
    private String attachmentUrl;
    private String attachmentName;
    private String attachmentContentType;
    private Long attachmentSize;
    private LocalDateTime sentAt;
    private int unreadCount;

    public static ChatMessageResponse from(ChatMessage message, String senderName) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoomId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .content(message.getContent())
                .messageType(message.getMessageType() != null ? message.getMessageType().name() : "TEXT")
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentName(message.getAttachmentName())
                .attachmentContentType(message.getAttachmentContentType())
                .attachmentSize(message.getAttachmentSize())
                .sentAt(message.getSentAt())
                .unreadCount(0)
                .build();
    }

    public static ChatMessageResponse from(ChatMessage message, String senderName, String senderProfileImage, int unreadCount) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoomId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .senderProfileImage(senderProfileImage)
                .content(message.getContent())
                .messageType(message.getMessageType() != null ? message.getMessageType().name() : "TEXT")
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentName(message.getAttachmentName())
                .attachmentContentType(message.getAttachmentContentType())
                .attachmentSize(message.getAttachmentSize())
                .sentAt(message.getSentAt())
                .unreadCount(unreadCount)
                .build();
    }
}
