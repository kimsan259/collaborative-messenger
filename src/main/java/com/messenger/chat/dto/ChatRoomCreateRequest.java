package com.messenger.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ============================================================
 * ChatRoomCreateRequest - 채팅방 생성 요청 DTO
 * ============================================================
 *
 * 【초대 방식】
 * 사용자의 아이디(username)를 쉼표로 구분하여 입력합니다.
 * 예: "kimsan4515, hong123"
 * ============================================================
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateRequest {

    /** 채팅방 이름 (그룹 채팅방인 경우) */
    @NotBlank(message = "채팅방 이름을 입력해주세요.")
    private String name;

    /** 채팅방 유형: "DIRECT" 또는 "GROUP" */
    private String roomType;

    /** 초대할 사용자 아이디(username) 목록 */
    @NotEmpty(message = "초대할 멤버를 1명 이상 입력해주세요.")
    private List<String> memberUsernames;
}
