package com.messenger.chat.dto;

import com.messenger.chat.entity.ChatRoomMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ChatRoomMemberResponse {

    private Long memberId;
    private Long userId;
    private String username;
    private String displayName;
    private String profileImage;
    private String joinedAt;

    public static ChatRoomMemberResponse from(ChatRoomMember member) {
        return ChatRoomMemberResponse.builder()
                .memberId(member.getId())
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .displayName(member.getUser().getDisplayName())
                .profileImage(member.getUser().getProfileImage())
                .joinedAt(member.getCreatedAt() != null ? member.getCreatedAt().toString() : null)
                .build();
    }
}
