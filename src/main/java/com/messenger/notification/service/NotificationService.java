package com.messenger.notification.service;

import com.messenger.notification.dto.NotificationResponse;
import com.messenger.notification.entity.Notification;
import com.messenger.notification.entity.NotificationType;
import com.messenger.notification.repository.NotificationRepository;
import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * NotificationService - 알림 관리 서비스
 * ============================================================
 *
 * 【역할】
 * 알림의 생성, 조회, 읽음 처리, 실시간 전송을 담당합니다.
 *
 * 【알림 전송 흐름】
 * 1. createAndSend() 호출 (비동기 - @Async)
 * 2. DB에 알림 저장
 * 3. WebSocket으로 해당 사용자에게 실시간 전송 (/user/{userId}/queue/notifications)
 *
 * 【비동기 처리하는 이유】
 * 알림 발송이 채팅 메시지 처리를 지연시키면 안 됩니다.
 * 예: 채팅에서 @멘션 감지 → 알림 저장 + WebSocket 전송을 별도 스레드에서 처리
 *     → 메인 채팅 흐름은 즉시 계속 진행
 *
 * 【WebSocket 알림 경로】
 * - /user/{userId}/queue/notifications
 *   → Spring STOMP의 사용자별 메시지 전송 기능 활용
 *   → SimpMessagingTemplate.convertAndSendToUser() 사용
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 【알림 생성 + 실시간 전송 (비동기)】
     *
     * 알림을 DB에 저장하고, WebSocket으로 실시간 전송합니다.
     * @Async("notificationExecutor"): 알림 전용 스레드풀에서 실행됩니다.
     *
     * @param recipientId 알림 받을 사용자 ID
     * @param type        알림 유형 (MENTION, REPORT, SYSTEM)
     * @param message     알림 메시지 내용
     * @param referenceId 관련 객체 ID (채팅방 ID, 리포트 ID 등)
     */
    @Async("notificationExecutor")
    @Transactional
    public void createAndSend(Long recipientId, NotificationType type, String message, Long referenceId) {
        try {
            // 1단계: DB에 알림 저장
            Notification notification = Notification.builder()
                    .recipientId(recipientId)
                    .type(type)
                    .message(message)
                    .referenceId(referenceId)
                    .build();

            notificationRepository.save(notification);
            log.info("[알림 생성] 수신자ID={}, 유형={}, 메시지={}", recipientId, type, message);

            // 2단계: WebSocket으로 실시간 전송
            // convertAndSendToUser: Spring이 자동으로 /user/{userId}/queue/notifications 경로로 라우팅
            NotificationResponse response = NotificationResponse.from(notification);
            messagingTemplate.convertAndSendToUser(
                    recipientId.toString(),
                    "/queue/notifications",
                    response
            );

            log.debug("[알림 전송] WebSocket으로 전송 완료 - 수신자ID={}", recipientId);

        } catch (Exception e) {
            // 알림 발송 실패가 전체 시스템에 영향을 주면 안 되므로 로그만 남김
            log.error("[알림 전송 실패] 수신자ID={}, 에러={}", recipientId, e.getMessage(), e);
        }
    }

    /**
     * 사용자의 모든 알림 조회 (최신순).
     *
     * @param userId 사용자 ID
     * @return 알림 목록
     */
    public List<NotificationResponse> getNotifications(Long userId) {
        List<Notification> notifications = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId);

        return notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 읽지 않은 알림 수 조회.
     *
     * @param userId 사용자 ID
     * @return 읽지 않은 알림 수
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    /**
     * 특정 알림을 읽음 처리.
     * 본인의 알림만 읽음 처리할 수 있습니다.
     *
     * @param notificationId 알림 ID
     * @param userId 요청한 사용자 ID (소유권 검증용)
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getRecipientId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        notification.markAsRead();
        notificationRepository.save(notification);
        log.debug("[알림 읽음] 알림ID={}, 사용자ID={}", notificationId, userId);
    }

    /**
     * 사용자의 모든 알림을 읽음 처리.
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository
                .findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId);

        unread.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unread);

        log.info("[알림 전체 읽음] 사용자ID={}, 처리 건수={}", userId, unread.size());
    }
}
