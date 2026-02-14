package com.messenger.user.service;

import com.messenger.chat.service.ChatPresenceService;
import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import com.messenger.notification.entity.NotificationType;
import com.messenger.notification.service.NotificationService;
import com.messenger.user.dto.FriendshipResponse;
import com.messenger.user.entity.Friendship;
import com.messenger.user.entity.FriendshipStatus;
import com.messenger.user.entity.User;
import com.messenger.user.repository.FriendshipRepository;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * FriendshipService - 친구 관계 비즈니스 로직
 * ============================================================
 *
 * 【역할】
 * 친구 요청, 수락, 거절, 차단, 목록 조회 등 친구 관계 관리를 담당합니다.
 *
 * 【친구 관계 흐름】
 * 1. A가 B에게 친구 요청 (PENDING)
 * 2. B가 수락(ACCEPTED) 또는 거절(REJECTED)
 * 3. 수락 시 양방향 친구 관계 성립
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ChatPresenceService chatPresenceService;

    /**
     * 【친구 요청 보내기】
     */
    @Transactional
    public FriendshipResponse sendFriendRequest(Long requesterId, Long receiverId) {
        log.info("[친구 요청] 요청자ID={}, 수신자ID={}", requesterId, receiverId);

        // 자기 자신에게 요청 불가
        if (requesterId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.CANNOT_ADD_SELF);
        }

        // 수신자 존재 확인
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 이미 관계가 존재하는지 확인
        friendshipRepository.findByUsers(requesterId, receiverId).ifPresent(existing -> {
            if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new BusinessException(ErrorCode.ALREADY_FRIENDS);
            }
            if (existing.getStatus() == FriendshipStatus.PENDING) {
                throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_SENT);
            }
            if (existing.getStatus() == FriendshipStatus.REJECTED) {
                // 거절된 요청이 있으면 삭제 후 재요청 허용
                friendshipRepository.delete(existing);
            }
        });

        // 친구 요청 생성
        Friendship friendship = Friendship.builder()
                .requester(requester)
                .receiver(receiver)
                .status(FriendshipStatus.PENDING)
                .build();

        Friendship saved = friendshipRepository.save(friendship);
        log.info("[친구 요청] 생성 완료 - friendshipId={}", saved.getId());

        // 알림 전송
        notificationService.createAndSend(
                receiverId,
                NotificationType.FRIEND_REQUEST,
                requester.getDisplayName() + "님이 친구 요청을 보냈습니다.",
                saved.getId()
        );

        return FriendshipResponse.from(saved, requesterId);
    }

    /**
     * 【친구 요청 수락】
     */
    @Transactional
    public FriendshipResponse acceptFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        // 수신자만 수락 가능
        if (!friendship.getReceiver().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }

        friendship.accept();
        log.info("[친구 수락] friendshipId={}, 요청자={}, 수락자={}",
                friendshipId, friendship.getRequester().getId(), currentUserId);

        // 요청자에게 수락 알림
        notificationService.createAndSend(
                friendship.getRequester().getId(),
                NotificationType.FRIEND_ACCEPTED,
                friendship.getReceiver().getDisplayName() + "님이 친구 요청을 수락했습니다.",
                friendshipId
        );

        return FriendshipResponse.from(friendship, currentUserId);
    }

    /**
     * 【친구 요청 거절】
     */
    @Transactional
    public void rejectFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        // 수신자만 거절 가능
        if (!friendship.getReceiver().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }

        friendship.reject();
        log.info("[친구 거절] friendshipId={}, 거절자={}", friendshipId, currentUserId);
    }

    /**
     * 【친구 삭제 (관계 해제)】
     */
    @Transactional
    public void removeFriend(Long friendshipId, Long currentUserId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIENDSHIP_NOT_FOUND));

        // 관계의 당사자만 삭제 가능
        boolean isParticipant = friendship.getRequester().getId().equals(currentUserId)
                || friendship.getReceiver().getId().equals(currentUserId);
        if (!isParticipant) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        friendshipRepository.delete(friendship);
        log.info("[친구 삭제] friendshipId={}, 삭제자={}", friendshipId, currentUserId);
    }

    /**
     * 【내 친구 목록 조회 (수락된 관계만)】
     */
    public List<FriendshipResponse> getFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository
                .findAllByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED);

        return friendships.stream()
                .map(f -> {
                    Long friendId = f.getRequester().getId().equals(userId)
                            ? f.getReceiver().getId()
                            : f.getRequester().getId();
                    boolean online = chatPresenceService.isOnline(friendId);
                    return FriendshipResponse.from(f, userId, online);
                })
                .collect(Collectors.toList());
    }

    /**
     * 【내가 받은 대기 중인 친구 요청 목록】
     */
    public List<FriendshipResponse> getPendingReceivedRequests(Long userId) {
        List<Friendship> requests = friendshipRepository
                .findByReceiverIdAndStatus(userId, FriendshipStatus.PENDING);

        return requests.stream()
                .map(f -> FriendshipResponse.from(f, userId))
                .collect(Collectors.toList());
    }

    /**
     * 【내가 보낸 대기 중인 친구 요청 목록】
     */
    public List<FriendshipResponse> getPendingSentRequests(Long userId) {
        List<Friendship> requests = friendshipRepository
                .findByRequesterIdAndStatus(userId, FriendshipStatus.PENDING);

        return requests.stream()
                .map(f -> FriendshipResponse.from(f, userId))
                .collect(Collectors.toList());
    }

    /**
     * 【사용자 검색 (친구 추가용)】
     * username 또는 displayName으로 사용자를 검색합니다.
     */
    public List<FriendshipResponse> searchUsers(String keyword, Long currentUserId) {
        List<User> users = userRepository.findByUsernameContainingOrDisplayNameContaining(keyword, keyword);

        return users.stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .map(u -> {
                    // 이미 관계가 있는지 확인
                    var existing = friendshipRepository.findByUsers(currentUserId, u.getId());
                    if (existing.isPresent()) {
                        return FriendshipResponse.from(existing.get(), currentUserId);
                    }
                    // 관계 없는 사용자
                    return FriendshipResponse.builder()
                            .friendId(u.getId())
                            .friendUsername(u.getUsername())
                            .friendDisplayName(u.getDisplayName())
                            .status(null)
                            .isSentByMe(false)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
