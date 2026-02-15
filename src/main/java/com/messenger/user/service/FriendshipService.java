package com.messenger.user.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public FriendshipResponse sendFriendRequest(Long requesterId, Long receiverId) {
        if (requesterId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (receiverId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        log.info("[friend-request] requesterId={}, receiverId={}", requesterId, receiverId);

        if (requesterId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.CANNOT_ADD_SELF);
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        friendshipRepository.findByUsers(requesterId, receiverId).ifPresent(existing -> {
            if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new BusinessException(ErrorCode.ALREADY_FRIENDS);
            }
            if (existing.getStatus() == FriendshipStatus.PENDING) {
                throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_SENT);
            }
            if (existing.getStatus() == FriendshipStatus.REJECTED) {
                friendshipRepository.delete(existing);
            }
        });

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .receiver(receiver)
                .status(FriendshipStatus.PENDING)
                .build();

        Friendship saved = friendshipRepository.save(friendship);
        log.info("[friend-request] saved friendshipId={}", saved.getId());

        try {
            notificationService.createAndSend(
                    receiverId,
                    NotificationType.FRIEND_REQUEST,
                    requester.getDisplayName() + "님이 친구 요청을 보냈습니다.",
                    saved.getId()
            );
        } catch (Exception e) {
            // Do not fail friend request transaction because async notification enqueue failed.
            log.warn("[friend-request] notification enqueue failed. friendshipId={}, reason={}",
                    saved.getId(), e.getMessage());
        }

        return FriendshipResponse.from(saved, requesterId);
    }

    @Transactional
    public FriendshipResponse acceptFriendRequest(Long friendshipId, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friendship.getReceiver().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }

        friendship.accept();
        log.info("[friend-accept] friendshipId={}, requesterId={}, receiverId={}",
                friendshipId, friendship.getRequester().getId(), currentUserId);

        try {
            notificationService.createAndSend(
                    friendship.getRequester().getId(),
                    NotificationType.FRIEND_ACCEPTED,
                    friendship.getReceiver().getDisplayName() + "님이 친구 요청을 수락했습니다.",
                    friendshipId
            );
        } catch (Exception e) {
            log.warn("[friend-accept] notification enqueue failed. friendshipId={}, reason={}",
                    friendshipId, e.getMessage());
        }

        return FriendshipResponse.from(friendship, currentUserId);
    }

    @Transactional
    public void rejectFriendRequest(Long friendshipId, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friendship.getReceiver().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }

        friendship.reject();
        log.info("[friend-reject] friendshipId={}, userId={}", friendshipId, currentUserId);
    }

    @Transactional
    public void removeFriend(Long friendshipId, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIENDSHIP_NOT_FOUND));

        boolean isParticipant = friendship.getRequester().getId().equals(currentUserId)
                || friendship.getReceiver().getId().equals(currentUserId);
        if (!isParticipant) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        friendshipRepository.delete(friendship);
        log.info("[friend-remove] friendshipId={}, userId={}", friendshipId, currentUserId);
    }

    public List<FriendshipResponse> getFriends(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        List<Friendship> friendships = friendshipRepository
                .findAllByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED);

        return friendships.stream()
                .map(f -> FriendshipResponse.from(f, userId))
                .collect(Collectors.toList());
    }

    public List<FriendshipResponse> getPendingReceivedRequests(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        List<Friendship> requests = friendshipRepository
                .findByReceiverIdAndStatus(userId, FriendshipStatus.PENDING);

        return requests.stream()
                .map(f -> FriendshipResponse.from(f, userId))
                .collect(Collectors.toList());
    }

    public List<FriendshipResponse> getPendingSentRequests(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        List<Friendship> requests = friendshipRepository
                .findByRequesterIdAndStatus(userId, FriendshipStatus.PENDING);

        return requests.stream()
                .map(f -> FriendshipResponse.from(f, userId))
                .collect(Collectors.toList());
    }

    public List<FriendshipResponse> searchUsers(String keyword, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        List<User> users = userRepository.findByUsernameContainingOrDisplayNameContaining(keyword, keyword);

        return users.stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .map(u -> {
                    var existing = friendshipRepository.findByUsers(currentUserId, u.getId());
                    if (existing.isPresent()) {
                        return FriendshipResponse.from(existing.get(), currentUserId);
                    }
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
