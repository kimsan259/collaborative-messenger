package com.messenger.chat.service;

import com.messenger.chat.dto.ChatRoomResponse;
import com.messenger.chat.entity.ChatRoom;
import com.messenger.chat.entity.ChatRoomMember;
import com.messenger.chat.entity.RoomType;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * ============================================================
 * ChatRoomServiceTest - 채팅방 서비스 단위 테스트
 * ============================================================
 *
 * 【테스트 대상】
 * ChatRoomService의 비즈니스 로직
 * - 채팅방 생성 (+ 멤버 자동 추가)
 * - 채팅방 목록 조회
 * - 채팅방 상세 조회
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    // ===== 헬퍼 =====

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

    // ===== 테스트 케이스 =====

    @Test
    @DisplayName("채팅방 생성 시 멤버가 자동으로 추가됨")
    void createRoom_addsMembersAutomatically() {
        // given
        User user1 = createTestUser(1L, "user1");
        User user2 = createTestUser(2L, "user2");

        given(userRepository.findById(1L)).willReturn(Optional.of(user1));
        given(userRepository.findById(2L)).willReturn(Optional.of(user2));
        given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
            ChatRoom room = invocation.getArgument(0);
            return room;
        });

        // when
        chatRoomService.createRoom("테스트 채팅방", RoomType.GROUP, 1L, List.of(1L, 2L));

        // then: 채팅방 저장 + 멤버 2명 추가
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("존재하지 않는 채팅방 조회 → BusinessException 발생")
    void findRoomById_nonExisting_throwsException() {
        // given
        given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
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

        ChatRoomMember member = ChatRoomMember.builder()
                .chatRoom(room)
                .user(user)
                .build();

        given(chatRoomMemberRepository.findByUserId(1L)).willReturn(List.of(member));

        // when
        List<ChatRoomResponse> result = chatRoomService.findRoomsByUserId(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("테스트방");
    }
}
