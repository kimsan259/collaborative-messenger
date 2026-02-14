package com.messenger.chat.repository;

import com.messenger.chat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 * ChatRoomMemberRepository - 채팅방 멤버 데이터 접근 인터페이스
 * ============================================================
 */
@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    /** 특정 채팅방의 모든 멤버 조회 */
    List<ChatRoomMember> findByChatRoomId(Long chatRoomId);

    /** 특정 사용자가 참여한 모든 채팅방 조회 */
    List<ChatRoomMember> findByUserId(Long userId);

    /** 특정 사용자가 특정 채팅방의 멤버인지 확인 */
    boolean existsByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    /** 특정 사용자의 특정 채팅방 멤버십 조회 (읽음 처리용) */
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);
}
