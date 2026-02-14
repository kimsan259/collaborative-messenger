package com.messenger.user.entity;

import com.messenger.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * ============================================================
 * Friendship - 친구 관계 엔티티
 * ============================================================
 *
 * 【역할】
 * 두 사용자 간의 친구 관계를 관리합니다.
 * requester가 receiver에게 친구 요청을 보내는 단방향 레코드입니다.
 *
 * 【관계 구조】
 * - requester: 친구 요청을 보낸 사용자
 * - receiver:  친구 요청을 받은 사용자
 * - status:    PENDING → ACCEPTED / REJECTED / BLOCKED
 *
 * 【테이블】 friendships (Shard 0에 저장)
 * ============================================================
 */
@Entity
@Table(name = "friendships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_requester_receiver",
                columnNames = {"requester_id", "receiver_id"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Friendship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 친구 요청을 보낸 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** 친구 요청을 받은 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    /** 친구 관계 상태 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private FriendshipStatus status = FriendshipStatus.PENDING;

    /** 친구 요청을 수락합니다. */
    public void accept() {
        this.status = FriendshipStatus.ACCEPTED;
    }

    /** 친구 요청을 거절합니다. */
    public void reject() {
        this.status = FriendshipStatus.REJECTED;
    }

    /** 사용자를 차단합니다. */
    public void block() {
        this.status = FriendshipStatus.BLOCKED;
    }
}
