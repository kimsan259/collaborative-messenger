package com.messenger.notification.repository;

import com.messenger.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * ============================================================
 * NotificationRepository - 알림 데이터 접근
 * ============================================================
 *
 * 【주요 쿼리】
 * - findByRecipientIdOrderByCreatedAtDesc: 사용자의 모든 알림 (최신순)
 * - findByRecipientIdAndReadFalse:         읽지 않은 알림만 조회
 * - countByRecipientIdAndReadFalse:        읽지 않은 알림 수
 * ============================================================
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 특정 사용자의 알림 목록 (최신순) */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    /** 특정 사용자의 읽지 않은 알림 목록 */
    List<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(Long recipientId);

    /** 특정 사용자의 읽지 않은 알림 수 */
    long countByRecipientIdAndReadFalse(Long recipientId);
}
