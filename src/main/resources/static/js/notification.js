/**
 * ============================================================
 * notification.js - 실시간 알림 클라이언트
 * ============================================================
 *
 * 【이 파일이 하는 일】
 * 1. WebSocket을 통해 개인 알림 구독 (/user/queue/notifications)
 * 2. 새 알림 수신 시 화면에 토스트 표시
 * 3. 읽지 않은 알림 수 배지 업데이트
 * 4. 알림 목록 로드 및 읽음 처리
 *
 * 【사용 방법】
 * 이 파일은 알림이 필요한 페이지에서 포함합니다.
 * WebSocket 연결은 chat.js와 별도로, 알림 전용 구독을 설정합니다.
 *
 * 【필요한 전역 변수】
 * - USER_ID: 현재 로그인한 사용자 ID (선택, 없으면 API에서 세션으로 처리)
 * ============================================================
 */

var NotificationManager = (function () {

    // ===== 내부 상태 =====
    var unreadCount = 0;
    var $badge = null;

    /**
     * 알림 매니저 초기화.
     * 페이지 로드 시 호출하여 읽지 않은 알림 수를 표시합니다.
     */
    function init() {
        $badge = $('#notificationBadge');
        loadUnreadCount();
    }

    /**
     * 읽지 않은 알림 수를 서버에서 조회합니다.
     */
    function loadUnreadCount() {
        $.ajax({
            url: '/api/notifications/unread-count',
            method: 'GET',
            success: function (response) {
                if (response.data !== undefined) {
                    updateBadge(response.data);
                }
            },
            error: function () {
                // 알림 API 실패해도 페이지 동작에 영향 없음
                console.log('[알림] 읽지 않은 알림 수 조회 실패');
            }
        });
    }

    /**
     * WebSocket으로 수신한 알림을 처리합니다.
     * (chat.js에서 STOMP 연결 후 알림 구독을 추가할 때 사용)
     *
     * @param notification 알림 객체 { id, message, type, createdAt }
     */
    function onNotificationReceived(notification) {
        unreadCount++;
        updateBadge(unreadCount);
        showToast(notification.message);
    }

    /**
     * 알림 배지(숫자)를 업데이트합니다.
     *
     * @param count 읽지 않은 알림 수
     */
    function updateBadge(count) {
        unreadCount = count;
        if ($badge && $badge.length > 0) {
            if (count > 0) {
                $badge.text(count > 99 ? '99+' : count).show();
            } else {
                $badge.hide();
            }
        }
    }

    /**
     * 화면 상단에 토스트 알림을 표시합니다.
     *
     * @param message 알림 메시지 텍스트
     */
    function showToast(message) {
        // 기존 토스트 제거
        $('.notification-toast').remove();

        var toast = $(
            '<div class="notification-toast alert alert-info alert-dismissible fade show" ' +
            'style="position:fixed; top:20px; right:20px; z-index:9999; min-width:300px; ' +
            'box-shadow: 0 4px 12px rgba(0,0,0,0.15);">' +
            '<strong>새 알림</strong><br>' +
            '<span>' + escapeHtml(message) + '</span>' +
            '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>' +
            '</div>'
        );

        $('body').append(toast);

        // 5초 후 자동 제거
        setTimeout(function () {
            toast.fadeOut(300, function () { toast.remove(); });
        }, 5000);
    }

    /**
     * HTML 이스케이프 (XSS 방지)
     */
    function escapeHtml(text) {
        if (!text) return '';
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(text));
        return div.innerHTML;
    }

    // ===== 공개 API =====
    return {
        init: init,
        onNotificationReceived: onNotificationReceived,
        loadUnreadCount: loadUnreadCount
    };

})();

// 페이지 로드 시 자동 초기화
$(document).ready(function () {
    NotificationManager.init();
});
