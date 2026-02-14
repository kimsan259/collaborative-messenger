package com.messenger.chat.service;

import com.messenger.infrastructure.redis.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * ============================================================
 * ChatPresenceService - 사용자 접속 상태 관리 서비스
 * ============================================================
 *
 * 【역할】
 * Redis SET 자료구조를 사용하여 현재 온라인인 사용자를 관리합니다.
 *
 * 【Redis 구조】
 * Key: "online:users" (SET 타입)
 * Value: "user:42", "user:17", "user:5" ...
 *
 * SET 자료구조의 특징:
 * - 중복 불가: 같은 사용자를 여러 번 추가해도 하나만 저장
 * - O(1) 조회: 특정 사용자의 온라인 여부를 즉시 확인
 * - 전체 조회: 현재 온라인인 모든 사용자 목록을 한번에 가져올 수 있음
 *
 * 【호출 시점】
 * - setOnline():  WebSocket 연결 시 (WebSocketEventListener에서 호출)
 * - setOffline(): WebSocket 연결 해제 시
 * - isOnline():   채팅방 멤버 목록에서 온라인 상태 표시 시
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private final RedisCacheService redisCacheService;

    /** Redis에서 온라인 사용자 목록을 저장하는 SET의 키 */
    private static final String ONLINE_USERS_KEY = "online:users";

    /**
     * 사용자를 온라인으로 등록합니다.
     *
     * @param userId 사용자 ID
     */
    public void setOnline(Long userId) {
        redisCacheService.addToSet(ONLINE_USERS_KEY, "user:" + userId);
        log.info("[접속 상태] 온라인 등록 - 사용자ID={}", userId);
    }

    /**
     * 사용자를 오프라인으로 변경합니다.
     *
     * @param userId 사용자 ID
     */
    public void setOffline(Long userId) {
        redisCacheService.removeFromSet(ONLINE_USERS_KEY, "user:" + userId);
        log.info("[접속 상태] 오프라인 변경 - 사용자ID={}", userId);
    }

    /**
     * 특정 사용자가 현재 온라인인지 확인합니다.
     *
     * @param userId 사용자 ID
     * @return true면 온라인, false면 오프라인
     */
    public boolean isOnline(Long userId) {
        return redisCacheService.isSetMember(ONLINE_USERS_KEY, "user:" + userId);
    }

    /**
     * 현재 온라인인 모든 사용자 ID 목록을 조회합니다.
     *
     * @return 온라인 사용자 ID Set
     */
    public Set<Long> getOnlineUserIds() {
        Set<Object> members = redisCacheService.getSetMembers(ONLINE_USERS_KEY);
        if (members == null) return Set.of();

        return members.stream()
                .map(Object::toString)
                .filter(s -> s.startsWith("user:"))
                .map(s -> Long.parseLong(s.substring(5)))  // "user:42" → 42
                .collect(Collectors.toSet());
    }
}
