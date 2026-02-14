package com.messenger.chat.service;

import com.messenger.chat.dto.ChatMessageResponse;
import com.messenger.chat.entity.ChatMessage;
import com.messenger.chat.entity.ChatRoomMember;
import com.messenger.chat.repository.ChatMessageRepository;
import com.messenger.chat.repository.ChatRoomMemberRepository;
import com.messenger.infrastructure.sharding.ShardKeyHolder;
import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================
 * ChatMessageService - 채팅 메시지 비즈니스 로직
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;

    /**
     * 【채팅방의 메시지 이력 조회 (읽음 표시 포함)】
     */
    public List<ChatMessageResponse> getMessageHistory(Long chatRoomId, int page, int size) {
        log.debug("[메시지 이력 조회] 채팅방ID={}, 페이지={}, 크기={}", chatRoomId, page, size);

        List<ChatMessage> messages;
        try {
            ShardKeyHolder.set(chatRoomId);
            Pageable pageable = PageRequest.of(page, size);
            messages = chatMessageRepository
                    .findByChatRoomIdOrderBySentAtDesc(chatRoomId, pageable);
        } finally {
            ShardKeyHolder.clear();
        }

        // 발신자 이름 조회
        List<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> senderNames;
        Map<Long, String> senderImages;
        try {
            ShardKeyHolder.set(0L);
            List<User> senders = userRepository.findAllById(senderIds);
            senderNames = senders.stream()
                    .collect(Collectors.toMap(User::getId, User::getDisplayName));
            senderImages = senders.stream()
                    .filter(u -> u.getProfileImage() != null)
                    .collect(Collectors.toMap(User::getId, User::getProfileImage));
        } finally {
            ShardKeyHolder.clear();
        }

        // 채팅방 멤버의 lastReadAt 목록 조회 (읽음 표시 계산용)
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(chatRoomId);

        // 메시지별 unreadCount 계산
        List<ChatMessageResponse> responses = messages.stream()
                .map(msg -> {
                    int unreadCount = 0;
                    if (msg.getSentAt() != null) {
                        for (ChatRoomMember m : members) {
                            if (m.getLastReadAt() == null || m.getLastReadAt().isBefore(msg.getSentAt())) {
                                unreadCount++;
                            }
                        }
                    }
                    return ChatMessageResponse.from(
                            msg,
                            senderNames.getOrDefault(msg.getSenderId(), "알 수 없음"),
                            senderImages.get(msg.getSenderId()),
                            unreadCount
                    );
                })
                .collect(Collectors.toList());

        Collections.reverse(responses);
        return responses;
    }

    /**
     * 【특정 사용자의 특정 날짜 메시지 조회 (업무일지 생성용)】
     */
    public List<ChatMessage> getMessagesBySenderAndDateRange(Long senderId, LocalDateTime start, LocalDateTime end) {
        return chatMessageRepository.findBySenderIdAndSentAtBetween(senderId, start, end);
    }
}
