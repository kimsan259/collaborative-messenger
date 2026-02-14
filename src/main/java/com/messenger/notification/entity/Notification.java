package com.messenger.notification.entity;

import com.messenger.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * ============================================================
 * Notification - 알림 엔티티
 * ============================================================
 *
 * 【역할】
 * 사용자에게 전달되는 알림(멘션, 시스템 알림 등)을 저장합니다.
 *
 * 【알림 발생 시점】
 * 1. 채팅에서 @멘션 → MENTION 타입 알림 생성
 * 2. 업무일지 자동 생성 완료 → REPORT 타입 알림 생성
 * 3. 시스템 공지 → SYSTEM 타입 알림 생성
 *
 * 【테이블 구조】
 * notifications
 * ├── id (PK)
 * ├── recipient_id   ← 알림을 받을 사용자 ID
 * ├── type           ← MENTION, REPORT, SYSTEM
 * ├── message        ← 알림 메시지 내용
 * ├── reference_id   ← 관련 객체 ID (채팅방 ID, 리포트 ID 등)
 * ├── is_read        ← 읽음 여부
 * ├── created_at     ← BaseEntity 상속
 * └── updated_at     ← BaseEntity 상속
 * ============================================================
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림을 받을 사용자 ID */
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    /** 알림 유형 (MENTION, REPORT, SYSTEM) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    /** 알림 메시지 내용 */
    @Column(nullable = false, length = 500)
    private String message;

    /**
     * 관련 객체 ID.
     * - MENTION 알림: 채팅방 ID (해당 채팅방으로 이동)
     * - REPORT 알림: 업무일지 ID (해당 업무일지로 이동)
     * - SYSTEM 알림: null
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /** 읽음 여부 (기본값: false = 안 읽음) */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /** 알림을 읽음 처리합니다 */
    public void markAsRead() {
        this.read = true;
    }
}
