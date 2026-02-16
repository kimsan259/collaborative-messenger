package com.messenger.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ============================================================
 * ChatMessageRequest - 채팅 메시지 전송 요청 DTO
 * ============================================================
 *
 * 【역할】
 * 클라이언트(브라우저)가 WebSocket을 통해 메시지를 보낼 때 사용하는 DTO입니다.
 *
 * 【사용되는 곳】
 * JavaScript(chat.js)에서 STOMP로 메시지를 보낼 때:
 *   stompClient.publish({
 *     destination: '/app/chat.send',
 *     body: JSON.stringify({ chatRoomId: 7, content: "안녕하세요" })
 *   });
 * ============================================================
 */
@Getter
@Setter           // WebSocket 메시지 역직렬화에 setter가 필요
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    /** 메시지를 보낼 채팅방 ID */
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;

    /** 메시지 내용 */
    @Size(max = 5000, message = "메시지는 5000자 이하여야 합니다.")
    private String content;

    /** 메시지 유형 (기본: TEXT) */
    private String messageType;

    private String attachmentUrl;
    private String attachmentName;
    private String attachmentContentType;
    private Long attachmentSize;
}
