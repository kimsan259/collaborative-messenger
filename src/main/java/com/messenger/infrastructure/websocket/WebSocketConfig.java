package com.messenger.infrastructure.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * ============================================================
 * WebSocketConfig - WebSocket + STOMP 프로토콜 설정
 * ============================================================
 *
 * 【WebSocket이란?】
 * HTTP는 클라이언트가 요청해야 서버가 응답하는 단방향 통신입니다.
 * WebSocket은 한번 연결하면 양쪽 모두 자유롭게 데이터를 주고받을 수 있는
 * 양방향(bidirectional) 통신 프로토콜입니다.
 * → 채팅 앱처럼 실시간 데이터가 필요한 곳에 필수!
 *
 * 【STOMP란?】
 * Simple Text Oriented Messaging Protocol의 약자.
 * WebSocket 위에서 동작하는 메시징 프로토콜로, 다음 기능을 제공합니다:
 *   - 목적지(destination) 기반 메시지 라우팅 ("/topic/chatroom/7")
 *   - 구독(subscribe): 특정 채널의 메시지를 받겠다고 등록
 *   - 발행(publish): 특정 채널로 메시지를 보냄
 *
 * 【메시지 흐름 요약】
 *
 *   [클라이언트A → 서버]
 *   목적지: /app/chat.send (접두사 /app이 붙으면 → @MessageMapping 메서드로 라우팅)
 *
 *   [서버 → 클라이언트B,C,D]
 *   목적지: /topic/chatroom/7 (접두사 /topic이 붙으면 → 구독자에게 브로드캐스트)
 *
 *   [서버 → 특정 클라이언트]
 *   목적지: /user/queue/notifications (접두사 /user → 특정 사용자에게만 전달)
 *
 * 【SockJS란?】
 * WebSocket을 지원하지 않는 구형 브라우저에서도 동작하게 하는 폴백(fallback) 라이브러리.
 * WebSocket이 안 되면 HTTP Long Polling 등으로 대체합니다.
 * ============================================================
 */
@Configuration
@EnableWebSocketMessageBroker  // WebSocket 메시지 브로커 기능 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 【메시지 브로커 설정】
     * 메시지가 어디로 가야 하는지 라우팅 규칙을 정의합니다.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 구독 주소 접두사 설정
        // /topic: 1:N 브로드캐스트 (채팅방 메시지 등)
        // /queue: 1:1 메시지 (개인 알림 등)
        config.enableSimpleBroker("/topic", "/queue");

        // 발행 주소 접두사 설정
        // 클라이언트가 /app으로 시작하는 목적지로 메시지를 보내면
        // @MessageMapping이 붙은 메서드가 처리합니다
        config.setApplicationDestinationPrefixes("/app");

        // 특정 사용자 전용 메시지 접두사
        // /user/queue/notifications → 해당 사용자에게만 전달
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 【WebSocket 엔드포인트 등록】
     * 클라이언트가 WebSocket 연결을 맺을 때 사용하는 URL을 설정합니다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")          // WebSocket 연결 URL: ws://localhost:8080/ws
                .setAllowedOriginPatterns("*")  // 모든 도메인에서 접근 허용 (개발용)
                // ★ HttpSessionHandshakeInterceptor: HTTP 세션의 속성(userId, displayName 등)을
                //    WebSocket 세션으로 복사합니다.
                //    이것이 없으면 ChatWebSocketController에서 사용자 정보를 알 수 없습니다!
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS();               // SockJS 폴백 활성화
    }
}
