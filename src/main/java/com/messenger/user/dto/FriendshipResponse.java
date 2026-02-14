package com.messenger.user.dto;

import com.messenger.user.entity.Friendship;
import com.messenger.user.entity.FriendshipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ============================================================
 * FriendshipResponse - 친구 관계 응답 DTO
 * ============================================================
 */
@Getter
@Builder
@AllArgsConstructor
public class FriendshipResponse {

    private Long friendshipId;
    private Long friendId;
    private String friendUsername;
    private String friendDisplayName;
    private String friendProfileImage;
    private boolean online;
    private FriendshipStatus status;
    private boolean isSentByMe;
    private LocalDateTime createdAt;

    public static FriendshipResponse from(Friendship friendship, Long currentUserId) {
        return from(friendship, currentUserId, false);
    }

    public static FriendshipResponse from(Friendship friendship, Long currentUserId, boolean online) {
        boolean sentByMe = friendship.getRequester().getId().equals(currentUserId);
        var friend = sentByMe ? friendship.getReceiver() : friendship.getRequester();

        return FriendshipResponse.builder()
                .friendshipId(friendship.getId())
                .friendId(friend.getId())
                .friendUsername(friend.getUsername())
                .friendDisplayName(friend.getDisplayName())
                .friendProfileImage(friend.getProfileImage())
                .online(online)
                .status(friendship.getStatus())
                .isSentByMe(sentByMe)
                .createdAt(friendship.getCreatedAt())
                .build();
    }
}
