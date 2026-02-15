package com.messenger.infrastructure.websocket;

import com.messenger.user.entity.UserStatus;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UserRepository userRepository;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(accessor);

        log.info("[WebSocket connect] sessionId={}, userId={}", accessor.getSessionId(), userId);

        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> user.updateStatus(UserStatus.ONLINE));
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(accessor);

        log.info("[WebSocket disconnect] sessionId={}, userId={}", accessor.getSessionId(), userId);

        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> user.updateStatus(UserStatus.OFFLINE));
        }
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        if (accessor.getSessionAttributes() == null) {
            return null;
        }
        Object value = accessor.getSessionAttributes().get("userId");
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
