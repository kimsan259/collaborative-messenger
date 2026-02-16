package com.messenger.chat.service;

import com.messenger.chat.dto.ChatRoomCreateRequest;
import com.messenger.chat.dto.ChatRoomResponse;
import com.messenger.chat.entity.ChatMessage;
import com.messenger.chat.entity.ChatRoom;
import com.messenger.chat.entity.ChatRoomMember;
import com.messenger.chat.entity.RoomType;
import com.messenger.chat.repository.ChatMessageRepository;
import com.messenger.chat.repository.ChatRoomMemberRepository;
import com.messenger.chat.repository.ChatRoomRepository;
import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import com.messenger.infrastructure.sharding.ShardKeyHolder;
import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.messenger.chat.dto.ChatRoomMemberResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * ChatRoomService - 채팅방 관리 비즈니스 로직
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 【채팅방 생성】
     */
    @Transactional
    public ChatRoomResponse createRoom(ChatRoomCreateRequest request, Long creatorId) {
        log.info("[채팅방 생성] 이름={}, 유형={}, 생성자ID={}", request.getName(), request.getRoomType(), creatorId);

        RoomType roomType = "DIRECT".equalsIgnoreCase(request.getRoomType())
                ? RoomType.DIRECT : RoomType.GROUP;

        ChatRoom chatRoom = ChatRoom.builder()
                .name(request.getName())
                .roomType(roomType)
                .build();
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        addMemberToRoom(savedRoom, creator);

        for (String username : request.getMemberUsernames()) {
            String trimmed = username.trim();
            if (trimmed.isEmpty()) continue;

            User member = userRepository.findByUsername(trimmed)
                    .orElseThrow(() -> {
                        log.warn("[채팅방 생성] 존재하지 않는 사용자: {}", trimmed);
                        return new BusinessException(ErrorCode.USER_NOT_FOUND);
                    });

            if (!member.getId().equals(creatorId)) {
                addMemberToRoom(savedRoom, member);
            }
        }

        int memberCount = chatRoomMemberRepository.countByChatRoomId(savedRoom.getId());
        log.info("[채팅방 생성 완료] 채팅방ID={}, 이름={}, 멤버수={}", savedRoom.getId(), savedRoom.getName(), memberCount);

        return ChatRoomResponse.from(savedRoom, memberCount);
    }

    /**
     * 【특정 사용자의 채팅방 목록 조회 (unreadCount + lastMessage 포함)】
     */
    public List<ChatRoomResponse> findRoomsByUserId(Long userId) {
        List<ChatRoomMember> memberships = chatRoomMemberRepository.findByUserIdWithChatRoom(userId);

        return memberships.stream()
                .map(membership -> {
                    ChatRoom room = membership.getChatRoom();
                    int memberCount = chatRoomMemberRepository.countByChatRoomId(room.getId());

                    // 안 읽은 메시지 수 계산
                    long unreadCount = getUnreadCount(room.getId(), membership.getLastReadAt());

                    // 마지막 메시지 가져오기
                    String lastMessage = null;
                    String lastMessageTime = null;
                    try {
                        ShardKeyHolder.set(room.getId());
                        List<ChatMessage> lastMessages = chatMessageRepository
                                .findTop1ByChatRoomIdOrderBySentAtDesc(room.getId());
                        if (!lastMessages.isEmpty()) {
                            ChatMessage last = lastMessages.get(0);
                            lastMessage = last.getContent();
                            if (lastMessage != null && lastMessage.length() > 30) {
                                lastMessage = lastMessage.substring(0, 30) + "...";
                            }
                            if (last.getSentAt() != null) {
                                lastMessageTime = last.getSentAt().format(TIME_FORMATTER);
                            }
                        }
                    } finally {
                        ShardKeyHolder.clear();
                    }

                    // DM 방일 경우 상대방 이름을 채팅방 이름으로 사용
                    String displayName = room.getName();
                    if (room.getRoomType() == RoomType.DIRECT) {
                        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(room.getId());
                        for (ChatRoomMember m : members) {
                            if (!m.getUser().getId().equals(userId)) {
                                displayName = m.getUser().getDisplayName();
                                break;
                            }
                        }
                    }

                    return ChatRoomResponse.builder()
                            .id(room.getId())
                            .name(displayName)
                            .roomType(room.getRoomType().name())
                            .memberCount(memberCount)
                            .unreadCount(unreadCount)
                            .lastMessage(lastMessage)
                            .lastMessageTime(lastMessageTime)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 【채팅방 정보 조회】
     */
    public ChatRoomResponse findRoomById(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        int memberCount = chatRoomMemberRepository.countByChatRoomId(roomId);
        return ChatRoomResponse.from(room, memberCount);
    }

    /**
     * 【채팅방 읽음 처리】
     * 사용자가 채팅방에 입장하면 lastReadAt을 현재 시각으로 갱신합니다.
     */
    @Transactional
    public void markAsRead(Long roomId, Long userId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElse(null);
        if (member != null) {
            member.markAsRead();
            chatRoomMemberRepository.save(member);
            log.debug("[읽음 처리] 채팅방ID={}, 사용자ID={}", roomId, userId);
        }
    }

    /**
     * 【특정 메시지의 안 읽은 사람 수 계산】
     * 채팅방 멤버 중 lastReadAt이 메시지 sentAt보다 이전인 사람 수
     */
    public int getUnreadMemberCount(Long roomId, LocalDateTime sentAt) {
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        int unreadCount = 0;
        for (ChatRoomMember m : members) {
            if (m.getLastReadAt() == null || m.getLastReadAt().isBefore(sentAt)) {
                unreadCount++;
            }
        }
        return unreadCount;
    }

    /**
     * 【1:1 DM 채팅방 생성 또는 기존 방 조회】
     */
    @Transactional
    public ChatRoomResponse getOrCreateDirectRoom(Long currentUserId, Long targetUserId) {
        // 기존 DM 방이 있는지 확인
        var existingRoom = chatRoomRepository.findDirectRoomBetweenUsers(
                RoomType.DIRECT, currentUserId, targetUserId);

        if (existingRoom.isPresent()) {
            ChatRoom room = existingRoom.get();
            int memberCount = chatRoomMemberRepository.countByChatRoomId(room.getId());
            return ChatRoomResponse.from(room, memberCount);
        }

        // 없으면 새로 생성
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String roomName = currentUser.getDisplayName() + ", " + targetUser.getDisplayName();

        ChatRoom chatRoom = ChatRoom.builder()
                .name(roomName)
                .roomType(RoomType.DIRECT)
                .build();
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        addMemberToRoom(savedRoom, currentUser);
        addMemberToRoom(savedRoom, targetUser);

        log.info("[DM 채팅방 생성] 채팅방ID={}, {}↔{}", savedRoom.getId(),
                currentUser.getDisplayName(), targetUser.getDisplayName());

        return ChatRoomResponse.from(savedRoom, 2);
    }

    /** 안 읽은 메시지 수를 계산합니다. */
    private long getUnreadCount(Long roomId, LocalDateTime lastReadAt) {
        try {
            ShardKeyHolder.set(roomId);
            if (lastReadAt == null) {
                return chatMessageRepository.countByChatRoomId(roomId);
            }
            return chatMessageRepository.countByChatRoomIdAndSentAtAfter(roomId, lastReadAt);
        } finally {
            ShardKeyHolder.clear();
        }
    }

    /**
     * 【채팅방 멤버 목록 조회】
     */
    public List<ChatRoomMemberResponse> getRoomMembers(Long roomId) {
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        return members.stream()
                .map(ChatRoomMemberResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 【채팅방에 멤버 초대】
     */
    @Transactional
    public void inviteMember(Long roomId, String username) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (chatRoomMemberRepository.existsByChatRoomIdAndUserId(roomId, user.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_TEAM_MEMBER);
        }

        addMemberToRoom(room, user);
        log.info("[멤버 초대] 채팅방ID={}, 사용자={}", roomId, username);
    }

    /**
     * 【채팅방에서 멤버 강퇴】
     */
    @Transactional
    public void removeMember(Long roomId, Long targetUserId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER));

        chatRoomMemberRepository.delete(member);
        log.info("[멤버 강퇴] 채팅방ID={}, 대상사용자ID={}", roomId, targetUserId);
    }

    /**
     * 【채팅방 나가기】
     */
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER));

        chatRoomMemberRepository.delete(member);
        log.info("[채팅방 나가기] 채팅방ID={}, 사용자ID={}", roomId, userId);

        // 남은 멤버가 없으면 채팅방 삭제
        List<ChatRoomMember> remaining = chatRoomMemberRepository.findByChatRoomId(roomId);
        if (remaining.isEmpty()) {
            chatRoomRepository.deleteById(roomId);
            log.info("[채팅방 삭제] 채팅방ID={} - 멤버 없음", roomId);
        }
    }

    /** 채팅방에 멤버를 추가하는 내부 헬퍼 메서드 */
    private void addMemberToRoom(ChatRoom chatRoom, User user) {
        ChatRoomMember member = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .user(user)
                .build();
        chatRoomMemberRepository.save(member);
    }
}
