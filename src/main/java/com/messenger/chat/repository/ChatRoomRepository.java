package com.messenger.chat.repository;

import com.messenger.chat.entity.ChatRoom;
import com.messenger.chat.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================================
 * ChatRoomRepository - 채팅방 데이터 접근 인터페이스
 * ============================================================
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /** 두 사용자 간의 기존 DIRECT 채팅방을 조회합니다. */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.roomType = :roomType " +
           "AND cr.id IN (SELECT m1.chatRoom.id FROM ChatRoomMember m1 WHERE m1.user.id = :userId1) " +
           "AND cr.id IN (SELECT m2.chatRoom.id FROM ChatRoomMember m2 WHERE m2.user.id = :userId2)")
    Optional<ChatRoom> findDirectRoomBetweenUsers(@Param("roomType") RoomType roomType,
                                                   @Param("userId1") Long userId1,
                                                   @Param("userId2") Long userId2);
}
