package com.messenger.user.repository;

import com.messenger.user.entity.Friendship;
import com.messenger.user.entity.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 * FriendshipRepository - 친구 관계 데이터 접근
 * ============================================================
 */
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** 두 사용자 간의 친구 관계 조회 (방향 무관) */
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester.id = :userId1 AND f.receiver.id = :userId2) OR " +
            "(f.requester.id = :userId2 AND f.receiver.id = :userId1)")
    Optional<Friendship> findByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /** 내가 받은 친구 요청 목록 (대기 중) */
    List<Friendship> findByReceiverIdAndStatus(Long receiverId, FriendshipStatus status);

    /** 내가 보낸 친구 요청 목록 (대기 중) */
    List<Friendship> findByRequesterIdAndStatus(Long requesterId, FriendshipStatus status);

    /** 특정 사용자의 수락된 친구 목록 (내가 보냈거나 받은 것 모두) */
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester.id = :userId OR f.receiver.id = :userId) AND f.status = :status")
    List<Friendship> findAllByUserIdAndStatus(@Param("userId") Long userId, @Param("status") FriendshipStatus status);

    /** 두 사용자 간에 이미 관계가 존재하는지 확인 */
    @Query("SELECT COUNT(f) > 0 FROM Friendship f WHERE " +
            "(f.requester.id = :userId1 AND f.receiver.id = :userId2) OR " +
            "(f.requester.id = :userId2 AND f.receiver.id = :userId1)")
    boolean existsByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /** 특정 사용자와 여러 상대방 간의 친구 관계를 일괄 조회 (N+1 방지) */
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester.id = :userId AND f.receiver.id IN :otherIds) OR " +
            "(f.receiver.id = :userId AND f.requester.id IN :otherIds)")
    List<Friendship> findAllByUserAndOtherUsers(@Param("userId") Long userId, @Param("otherIds") List<Long> otherIds);
}
