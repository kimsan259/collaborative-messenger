package com.messenger.notification.service;

import com.messenger.common.exception.BusinessException;
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

/**
 * NotificationServiceTest - 알림 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

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

    @Test
    @DisplayName("알림 생성 시 DB 저장 + WebSocket 전송")
    void createAndSend_savesAndSendsViaWebSocket() {
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        notificationService.createAndSend(1L, NotificationType.MENTION, "홍길동님이 멘션했습니다", 5L);

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(
                any(String.class), any(String.class), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("사용자의 알림 목록 조회")
    void getNotifications_returnsNotificationList() {
        List<Notification> notifications = List.of(
                createTestNotification(1L, 42L, false),
                createTestNotification(2L, 42L, true));
        given(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(42L))
                .willReturn(notifications);

        List<NotificationResponse> result = notificationService.getNotifications(42L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMessage()).isEqualTo("테스트 알림 메시지");
    }

    @Test
    @DisplayName("읽지 않은 알림 수 조회")
    void getUnreadCount_returnsCount() {
        given(notificationRepository.countByRecipientIdAndReadFalse(42L)).willReturn(5L);

        long count = notificationService.getUnreadCount(42L);

        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("특정 알림 읽음 처리 - 본인 알림")
    void markAsRead_ownNotification_marksAsRead() {
        Notification notification = createTestNotification(1L, 42L, false);
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

        notificationService.markAsRead(1L, 42L);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("특정 알림 읽음 처리 - 타인 알림이면 예외 발생")
    void markAsRead_otherUserNotification_throwsException() {
        Notification notification = createTestNotification(1L, 42L, false);
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 99L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 시 예외 발생")
    void markAsRead_nonExistingNotification_throwsException() {
        given(notificationRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L, 42L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("모든 알림 일괄 읽음 처리")
    void markAllAsRead_marksAllUnreadNotifications() {
        List<Notification> unreadNotifications = List.of(
                createTestNotification(1L, 42L, false),
                createTestNotification(2L, 42L, false),
                createTestNotification(3L, 42L, false));
        given(notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(42L))
                .willReturn(unreadNotifications);

        notificationService.markAllAsRead(42L);

        assertThat(unreadNotifications).allMatch(Notification::isRead);
        verify(notificationRepository).saveAll(unreadNotifications);
    }
}
