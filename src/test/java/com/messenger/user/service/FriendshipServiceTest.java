package com.messenger.user.service;

import com.messenger.common.exception.BusinessException;
import com.messenger.notification.service.NotificationService;
import com.messenger.user.dto.FriendshipResponse;
import com.messenger.user.entity.Friendship;
import com.messenger.user.entity.FriendshipStatus;
import com.messenger.user.entity.User;
import com.messenger.user.entity.UserRole;
import com.messenger.user.entity.UserStatus;
import com.messenger.user.repository.FriendshipRepository;
import com.messenger.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * FriendshipServiceTest - 친구 관계 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FriendshipService friendshipService;

    private User createUser(Long id, String username) {
        return User.builder()
                .id(id).username(username).password("encoded")
                .displayName(username).email(username + "@test.com")
                .status(UserStatus.OFFLINE).role(UserRole.MEMBER)
                .build();
    }

    @Test
    @DisplayName("친구 요청 전송 성공")
    void sendFriendRequest_success() {
        User requester = createUser(1L, "user1");
        User receiver = createUser(2L, "user2");

        given(userRepository.findById(1L)).willReturn(Optional.of(requester));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendshipRepository.findByUsers(1L, 2L)).willReturn(Optional.empty());
        given(friendshipRepository.save(any(Friendship.class))).willAnswer(inv -> {
            Friendship f = inv.getArgument(0);
            return Friendship.builder()
                    .id(10L).requester(f.getRequester()).receiver(f.getReceiver())
                    .status(f.getStatus()).build();
        });

        FriendshipResponse result = friendshipService.sendFriendRequest(1L, 2L);

        assertThat(result).isNotNull();
        verify(friendshipRepository).save(any(Friendship.class));
    }

    @Test
    @DisplayName("자기 자신에게 친구 요청 시 예외 발생")
    void sendFriendRequest_toSelf_throwsException() {
        assertThatThrownBy(() -> friendshipService.sendFriendRequest(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("이미 친구인 경우 친구 요청 시 예외 발생")
    void sendFriendRequest_alreadyFriends_throwsException() {
        User requester = createUser(1L, "user1");
        User receiver = createUser(2L, "user2");
        Friendship existing = Friendship.builder()
                .id(10L).requester(requester).receiver(receiver)
                .status(FriendshipStatus.ACCEPTED).build();

        given(userRepository.findById(1L)).willReturn(Optional.of(requester));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendshipRepository.findByUsers(1L, 2L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(1L, 2L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("친구 요청 수락 성공")
    void acceptFriendRequest_success() {
        User requester = createUser(1L, "user1");
        User receiver = createUser(2L, "user2");
        Friendship friendship = Friendship.builder()
                .id(10L).requester(requester).receiver(receiver)
                .status(FriendshipStatus.PENDING).build();

        given(friendshipRepository.findById(10L)).willReturn(Optional.of(friendship));

        FriendshipResponse result = friendshipService.acceptFriendRequest(10L, 2L);

        assertThat(result).isNotNull();
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @Test
    @DisplayName("친구 요청 수락 - 수신자가 아닌 경우 예외 발생")
    void acceptFriendRequest_notReceiver_throwsException() {
        User requester = createUser(1L, "user1");
        User receiver = createUser(2L, "user2");
        Friendship friendship = Friendship.builder()
                .id(10L).requester(requester).receiver(receiver)
                .status(FriendshipStatus.PENDING).build();

        given(friendshipRepository.findById(10L)).willReturn(Optional.of(friendship));

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(10L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("친구 요청 거절 성공")
    void rejectFriendRequest_success() {
        User requester = createUser(1L, "user1");
        User receiver = createUser(2L, "user2");
        Friendship friendship = Friendship.builder()
                .id(10L).requester(requester).receiver(receiver)
                .status(FriendshipStatus.PENDING).build();

        given(friendshipRepository.findById(10L)).willReturn(Optional.of(friendship));

        friendshipService.rejectFriendRequest(10L, 2L);

        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.REJECTED);
    }

    @Test
    @DisplayName("친구 삭제 - 참여자가 아닌 경우 예외 발생")
    void removeFriend_notParticipant_throwsException() {
        User requester = createUser(1L, "user1");
        User receiver = createUser(2L, "user2");
        Friendship friendship = Friendship.builder()
                .id(10L).requester(requester).receiver(receiver)
                .status(FriendshipStatus.ACCEPTED).build();

        given(friendshipRepository.findById(10L)).willReturn(Optional.of(friendship));

        assertThatThrownBy(() -> friendshipService.removeFriend(10L, 99L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("친구 목록 조회")
    void getFriends_returnsFriendList() {
        User user1 = createUser(1L, "user1");
        User user2 = createUser(2L, "user2");
        Friendship friendship = Friendship.builder()
                .id(10L).requester(user1).receiver(user2)
                .status(FriendshipStatus.ACCEPTED).build();

        given(friendshipRepository.findAllByUserIdAndStatus(1L, FriendshipStatus.ACCEPTED))
                .willReturn(List.of(friendship));

        List<FriendshipResponse> result = friendshipService.getFriends(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("userId가 null이면 예외 발생")
    void getFriends_nullUserId_throwsException() {
        assertThatThrownBy(() -> friendshipService.getFriends(null))
                .isInstanceOf(BusinessException.class);
    }
}
