package com.messenger.infrastructure.sharding;

import lombok.extern.slf4j.Slf4j;

/**
 * ============================================================
 * ShardKeyHolder - 샤드 키를 스레드별로 보관하는 유틸리티
 * ============================================================
 *
 * 【역할】
 * 현재 스레드에서 사용할 샤드 키(chatRoomId)를 저장합니다.
 * ShardingDataSourceRouter가 이 값을 읽어 어떤 MySQL에 쿼리할지 결정합니다.
 *
 * 【ThreadLocal이란?】
 * 각 스레드가 독립적으로 값을 가지는 저장소입니다.
 *
 * 예시:
 *   스레드A: ShardKeyHolder.set(4L) → 스레드A에서만 4L이 보임
 *   스레드B: ShardKeyHolder.set(7L) → 스레드B에서만 7L이 보임
 *   → 동시에 요청을 처리하는 스레드들이 서로의 샤드 키에 영향을 주지 않음
 *
 * 【사용 순서 (매우 중요!)】
 * 1. ShardKeyHolder.set(chatRoomId)    ← 쿼리 전에 설정
 * 2. repository.save(message)           ← 이때 라우팅됨
 * 3. ShardKeyHolder.clear()             ← ★ 반드시 해제! (메모리 누수 방지)
 *
 * 【clear()를 안 하면 어떤 일이?】
 * 톰캣/Kafka는 스레드풀을 사용합니다. 스레드가 재사용될 때
 * 이전 요청의 샤드 키가 남아있어 엉뚱한 DB에 쿼리가 실행됩니다!
 * → try-finally 블록에서 반드시 clear()를 호출해야 합니다.
 * ============================================================
 */
@Slf4j
public class ShardKeyHolder {

    /** 스레드별 샤드 키 저장소 */
    private static final ThreadLocal<Long> SHARD_KEY = new ThreadLocal<>();

    /**
     * 현재 스레드의 샤드 키를 설정합니다.
     *
     * @param chatRoomId 채팅방 ID (샤드 키)
     */
    public static void set(Long chatRoomId) {
        SHARD_KEY.set(chatRoomId);
        log.debug("[ShardKeyHolder] 샤드 키 설정: chatRoomId={}", chatRoomId);
    }

    /**
     * 현재 스레드의 샤드 키를 조회합니다.
     *
     * @return 설정된 chatRoomId (없으면 null)
     */
    public static Long get() {
        return SHARD_KEY.get();
    }

    /**
     * ★ 현재 스레드의 샤드 키를 반드시 제거합니다.
     * try-finally 블록에서 호출해야 합니다!
     *
     * 예시:
     *   try {
     *       ShardKeyHolder.set(chatRoomId);
     *       repository.save(message);
     *   } finally {
     *       ShardKeyHolder.clear();  // ★ 반드시!
     *   }
     */
    public static void clear() {
        Long previousKey = SHARD_KEY.get();
        SHARD_KEY.remove();
        if (previousKey != null) {
            log.debug("[ShardKeyHolder] 샤드 키 해제: chatRoomId={}", previousKey);
        }
    }

    /**
     * 【샤드 이름 결정】
     * 현재 설정된 샤드 키를 기반으로 어떤 MySQL 인스턴스를 사용할지 결정합니다.
     *
     * 라우팅 규칙: chatRoomId % 2
     *   - 짝수 (0, 2, 4, 6...) → "shard_0" (MySQL 포트 3307)
     *   - 홀수 (1, 3, 5, 7...) → "shard_1" (MySQL 포트 3308)
     *   - 미설정 (null)        → "shard_0" (기본값)
     *
     * @return "shard_0" 또는 "shard_1"
     */
    public static String resolveShardName() {
        Long key = SHARD_KEY.get();

        if (key == null) {
            // 샤드 키가 설정되지 않으면 기본 샤드(shard_0)로 라우팅
            return "shard_0";
        }

        // chatRoomId % 2: 짝수면 shard_0, 홀수면 shard_1
        String shardName = "shard_" + (key % 2);
        log.debug("[ShardKeyHolder] 샤드 라우팅: chatRoomId={} → {} ({}%2={})",
                key, shardName, key, key % 2);
        return shardName;
    }
}
