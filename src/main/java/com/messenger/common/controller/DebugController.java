package com.messenger.common.controller;

import com.messenger.common.dto.ApiResponse;
import com.messenger.infrastructure.redis.RedisCacheService;
import com.messenger.infrastructure.sharding.ShardKeyHolder;
import com.messenger.chat.service.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================
 * DebugController - 개발/디버깅 전용 컨트롤러
 * ============================================================
 *
 * 【역할】
 * 개발 중에 시스템 내부 상태를 확인할 수 있는 API를 제공합니다.
 * 운영 환경에서는 비활성화됩니다 (@Profile("!prod")).
 *
 * 【엔드포인트 목록】
 * GET /debug/health              → 서비스 헬스 체크
 * GET /debug/shard/route/{id}    → 채팅방 ID로 샤드 라우팅 확인
 * GET /debug/redis/presence      → 현재 온라인 사용자 목록
 * GET /debug/thread-pools        → 스레드풀 상태 확인
 *
 * 【보안 주의】
 * @Profile("!prod") 으로 운영 환경에서 자동 비활성화됩니다.
 * 추가로 SecurityConfig에서 /debug/** 접근 제한 가능합니다.
 * ============================================================
 */
@Slf4j
@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
@Profile("!prod")  // 운영 환경에서는 이 컨트롤러가 빈으로 등록되지 않음
public class DebugController {

    private final RedisCacheService redisCacheService;
    private final ChatPresenceService chatPresenceService;

    /**
     * 【헬스 체크】
     * 서버가 정상 동작 중인지 확인합니다.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("javaVersion", System.getProperty("java.version"));
        health.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        health.put("freeMemoryMB", Runtime.getRuntime().freeMemory() / 1024 / 1024);
        health.put("totalMemoryMB", Runtime.getRuntime().totalMemory() / 1024 / 1024);

        return ResponseEntity.ok(ApiResponse.success("서버 상태 정보", health));
    }

    /**
     * 【샤드 라우팅 확인】
     * 특정 채팅방 ID가 어떤 샤드로 라우팅되는지 확인합니다.
     *
     * 예시: GET /debug/shard/route/7
     * 응답: { "chatRoomId": 7, "shardName": "shard_1", "formula": "7 % 2 = 1" }
     *
     * @param chatRoomId 확인할 채팅방 ID
     */
    @GetMapping("/shard/route/{chatRoomId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkShardRoute(
            @PathVariable Long chatRoomId) {

        Map<String, Object> result = new HashMap<>();
        result.put("chatRoomId", chatRoomId);
        result.put("shardIndex", chatRoomId % 2);
        result.put("shardName", "shard_" + (chatRoomId % 2));
        result.put("formula", chatRoomId + " % 2 = " + (chatRoomId % 2));

        log.info("[디버그] 샤드 라우팅 확인 - chatRoomId={} → shard_{}", chatRoomId, chatRoomId % 2);
        return ResponseEntity.ok(ApiResponse.success("샤드 라우팅 정보", result));
    }

    /**
     * 【온라인 사용자 확인】
     * Redis에 저장된 현재 온라인 사용자 목록을 확인합니다.
     */
    @GetMapping("/redis/presence")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkOnlineUsers() {
        Set<Long> onlineUsers = chatPresenceService.getOnlineUserIds();

        Map<String, Object> result = new HashMap<>();
        result.put("onlineUserCount", onlineUsers.size());
        result.put("onlineUserIds", onlineUsers);

        return ResponseEntity.ok(ApiResponse.success("온라인 사용자 정보", result));
    }

    /**
     * 【스레드풀 상태 확인】
     * 현재 JVM의 스레드 정보를 확인합니다.
     * 리포트 생성/알림 전용 스레드풀의 활성 상태를 파악할 때 유용합니다.
     */
    @GetMapping("/thread-pools")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkThreadPools() {
        Map<String, Object> result = new HashMap<>();

        // 현재 활성 스레드 수
        result.put("activeThreadCount", Thread.activeCount());

        // 전체 스레드 목록에서 커스텀 스레드풀 스레드 찾기
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);

        int reportThreads = 0;
        int notificationThreads = 0;
        for (Thread t : threads) {
            if (t != null) {
                if (t.getName().startsWith("report-")) reportThreads++;
                if (t.getName().startsWith("notification-")) notificationThreads++;
            }
        }

        result.put("reportExecutorActiveThreads", reportThreads);
        result.put("notificationExecutorActiveThreads", notificationThreads);

        return ResponseEntity.ok(ApiResponse.success("스레드풀 상태 정보", result));
    }

    /**
     * 【ShardKeyHolder 테스트】
     * ThreadLocal 기반 ShardKeyHolder의 동작을 테스트합니다.
     * 설정 → 확인 → 해제 과정을 한번에 보여줍니다.
     */
    @GetMapping("/shard/test/{chatRoomId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testShardKeyHolder(
            @PathVariable Long chatRoomId) {

        Map<String, Object> result = new HashMap<>();

        // 1. 현재 상태 (비어 있어야 함)
        result.put("step1_before", ShardKeyHolder.resolveShardName());

        // 2. 샤드 키 설정
        ShardKeyHolder.set(chatRoomId);
        result.put("step2_afterSet", ShardKeyHolder.resolveShardName());

        // 3. 샤드 키 해제 (★ 반드시 해제해야 함!)
        ShardKeyHolder.clear();
        result.put("step3_afterClear", ShardKeyHolder.resolveShardName());

        result.put("chatRoomId", chatRoomId);
        result.put("expectedShard", "shard_" + (chatRoomId % 2));

        return ResponseEntity.ok(ApiResponse.success("ShardKeyHolder 테스트 결과", result));
    }
}
