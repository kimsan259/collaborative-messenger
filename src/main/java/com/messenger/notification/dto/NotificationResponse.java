package com.messenger.notification.dto;

import com.messenger.notification.entity.Notification;
import com.messenger.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ============================================================
 * NotificationResponse - 알림 응답 DTO
 * ============================================================
 *
 * 【역할】
 * Notification 엔티티를 클라이언트에 전달하기 위한 형태로 변환합니다.
 * Entity를 직접 노출하지 않는 것이 보안과 유지보수에 유리합니다.
 * ============================================================
 */
@Getter
@Builder
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String message;
    private Long referenceId;
    private boolean read;
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환 팩토리 메서드.
     *
     * @param notification 알림 엔티티
     * @return 알림 응답 DTO
     */
    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
