package com.messenger.notification.service;

import com.messenger.notification.dto.NotificationResponse;
import com.messenger.notification.entity.Notification;
import com.messenger.notification.entity.NotificationType;
import com.messenger.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * ============================================================
 * NotificationServiceTest - 알림 서비스 단위 테스트
 * ============================================================
 *
 * 【테스트 대상】
 * NotificationService의 비즈니스 로직
 * - 알림 생성 + WebSocket 전송
 * - 알림 목록 조회
 * - 읽음 처리
 *
 * 【Mock 대상】
 * - NotificationRepository: DB 대신 Mock
 * - SimpMessagingTemplate: WebSocket 전송 대신 Mock
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    // ===== 헬퍼 =====

    private Notification createTestNotification(Long id, Long recipientId, boolean read) {
        return Notification.builder()
                .id(id)
                .recipientId(recipientId)
                .type(NotificationType.MENTION)
                .message("테스트 알림 메시지")
                .referenceId(1L)
                .read(read)
                .build();
    }

    // ===== 테스트 케이스 =====

    @Test
    @DisplayName("알림 생성 시 DB 저장 + WebSocket 전송")
    void createAndSend_savesAndSendsViaWebSocket() {
        // given
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        notificationService.createAndSend(1L, NotificationType.MENTION, "홍길동님이 멘션했습니다", 5L);

        // then: DB 저장이 호출되었는지 확인
        verify(notificationRepository).save(any(Notification.class));

        // then: WebSocket 전송이 호출되었는지 확인
        verify(messagingTemplate).convertAndSendToUser(
                any(String.class),
                any(String.class),
                any(NotificationResponse.class)
        );
    }

    @Test
    @DisplayName("사용자의 알림 목록 조회")
    void getNotifications_returnsNotificationList() {
        // given
        List<Notification> notifications = List.of(
                createTestNotification(1L, 42L, false),
                createTestNotification(2L, 42L, true)
        );
        given(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(42L))
                .willReturn(notifications);

        // when
        List<NotificationResponse> result = notificationService.getNotifications(42L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMessage()).isEqualTo("테스트 알림 메시지");
    }

    @Test
    @DisplayName("읽지 않은 알림 수 조회")
    void getUnreadCount_returnsCount() {
        // given
        given(notificationRepository.countByRecipientIdAndReadFalse(42L)).willReturn(5L);

        // when
        long count = notificationService.getUnreadCount(42L);

        // then
        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("특정 알림 읽음 처리")
    void markAsRead_existingNotification_marksAsRead() {
        // given
        Notification notification = createTestNotification(1L, 42L, false);
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

        // when
        notificationService.markAsRead(1L);

        // then: 읽음 처리 + 저장이 되었는지 확인
        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 → 아무 일도 안 함 (예외 없음)")
    void markAsRead_nonExistingNotification_doesNothing() {
        // given
        given(notificationRepository.findById(999L)).willReturn(Optional.empty());

        // when: 예외 없이 정상 처리되어야 함
        notificationService.markAsRead(999L);

        // then: save가 호출되지 않아야 함
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("모든 알림 일괄 읽음 처리")
    void markAllAsRead_marksAllUnreadNotifications() {
        // given
        List<Notification> unreadNotifications = List.of(
                createTestNotification(1L, 42L, false),
                createTestNotification(2L, 42L, false),
                createTestNotification(3L, 42L, false)
        );
        given(notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(42L))
                .willReturn(unreadNotifications);

        // when
        notificationService.markAllAsRead(42L);

        // then: 모든 알림이 읽음 처리됨
        assertThat(unreadNotifications).allMatch(Notification::isRead);
        verify(notificationRepository).saveAll(unreadNotifications);
    }
}
