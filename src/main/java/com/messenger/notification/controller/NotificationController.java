package com.messenger.notification.controller;

import com.messenger.common.dto.ApiResponse;
import com.messenger.notification.dto.NotificationResponse;
import com.messenger.notification.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 * NotificationController - 알림 REST API 컨트롤러
 * ============================================================
 *
 * 【엔드포인트 목록】
 * GET  /api/notifications              → 내 알림 목록 조회
 * GET  /api/notifications/unread-count  → 읽지 않은 알림 수 조회
 * POST /api/notifications/{id}/read     → 특정 알림 읽음 처리
 * POST /api/notifications/read-all      → 모든 알림 읽음 처리
 *
 * 【실시간 알림은 별도 경로】
 * 실시간 알림은 WebSocket /user/{userId}/queue/notifications 경로로 전달됩니다.
 * 이 컨트롤러는 HTTP REST API로 알림 목록/읽음 처리를 제공합니다.
 * ============================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 내 알림 목록 조회.
     * 세션에서 userId를 가져와 해당 사용자의 알림을 반환합니다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        List<NotificationResponse> notifications = notificationService.getNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success("알림 목록을 조회했습니다.", notifications));
    }

    /**
     * 읽지 않은 알림 수 조회.
     * 프론트엔드에서 알림 배지 숫자를 표시할 때 사용합니다.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success("읽지 않은 알림 수를 조회했습니다.", count));
    }

    /**
     * 특정 알림을 읽음 처리합니다.
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("알림을 읽음 처리했습니다."));
    }

    /**
     * 모든 알림을 읽음 처리합니다.
     */
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("모든 알림을 읽음 처리했습니다."));
    }
}
