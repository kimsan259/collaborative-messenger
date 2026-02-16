package com.messenger.chat.service;

import com.messenger.chat.dto.ChatRoomCreateRequest;
import com.messenger.chat.dto.ChatRoomResponse;
import com.messenger.chat.entity.ChatRoom;
import com.messenger.chat.entity.ChatRoomMember;
import com.messenger.chat.entity.RoomType;
import com.messenger.chat.repository.ChatMessageRepository;
import com.messenger.chat.repository.ChatRoomMemberRepository;
import com.messenger.chat.repository.ChatRoomRepository;
import com.messenger.common.exception.BusinessException;
import com.messenger.user.entity.User;
import com.messenger.user.entity.UserRole;
import com.messenger.user.entity.UserStatus;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * ChatRoomServiceTest - 채팅방 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private User createTestUser(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .password("encoded")
                .displayName(username)
                .email(username + "@test.com")
                .status(UserStatus.OFFLINE)
                .role(UserRole.MEMBER)
                .build();
    }

    @Test
    @DisplayName("채팅방 생성 시 멤버가 자동으로 추가됨")
    void createRoom_addsMembersAutomatically() {
        // given
        User creator = createTestUser(1L, "creator");
        User member = createTestUser(2L, "member");

        given(userRepository.findById(1L)).willReturn(Optional.of(creator));
        given(userRepository.findByUsername("member")).willReturn(Optional.of(member));
        given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
            ChatRoom room = invocation.getArgument(0);
            return room;
        });
        given(chatRoomMemberRepository.countByChatRoomId(any())).willReturn(2);

        ChatRoomCreateRequest request = new ChatRoomCreateRequest("테스트방", "GROUP", List.of("member"));

        // when
        ChatRoomResponse result = chatRoomService.createRoom(request, 1L);

        // then
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
        assertThat(result.getName()).isEqualTo("테스트방");
    }

    @Test
    @DisplayName("존재하지 않는 채팅방 조회 시 BusinessException 발생")
    void findRoomById_nonExisting_throwsException() {
        given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.findRoomById(999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("사용자가 참여한 채팅방 목록 조회")
    void findRoomsByUserId_returnsRoomList() {
        // given
        User user = createTestUser(1L, "user1");
        ChatRoom room = ChatRoom.builder()
                .id(1L)
                .name("테스트방")
                .roomType(RoomType.GROUP)
                .build();

        ChatRoomMember membership = ChatRoomMember.builder()
                .chatRoom(room)
                .user(user)
                .build();

        given(chatRoomMemberRepository.findByUserIdWithChatRoom(1L)).willReturn(List.of(membership));
        given(chatRoomMemberRepository.countByChatRoomId(1L)).willReturn(1);

        // when
        List<ChatRoomResponse> result = chatRoomService.findRoomsByUserId(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("테스트방");
    }

    @Test
    @DisplayName("채팅방 멤버 초대 - 이미 멤버인 경우 예외 발생")
    void inviteMember_alreadyMember_throwsException() {
        // given
        ChatRoom room = ChatRoom.builder().id(1L).name("방").roomType(RoomType.GROUP).build();
        User user = createTestUser(10L, "invited");
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findByUsername("invited")).willReturn(Optional.of(user));
        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(1L, 10L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> chatRoomService.inviteMember(1L, "invited"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("채팅방 나가기 - 마지막 멤버가 나가면 채팅방 삭제")
    void leaveRoom_lastMember_deletesRoom() {
        // given
        User user = createTestUser(1L, "user1");
        ChatRoom room = ChatRoom.builder().id(1L).name("방").roomType(RoomType.GROUP).build();
        ChatRoomMember member = ChatRoomMember.builder().chatRoom(room).user(user).build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(1L, 1L)).willReturn(Optional.of(member));
        given(chatRoomMemberRepository.findByChatRoomId(1L)).willReturn(List.of());

        // when
        chatRoomService.leaveRoom(1L, 1L);

        // then
        verify(chatRoomMemberRepository).delete(member);
        verify(chatRoomRepository).deleteById(1L);
    }
}
