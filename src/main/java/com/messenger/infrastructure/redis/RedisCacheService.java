package com.messenger.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================
 * RedisCacheService - Redis 캐시 유틸리티 서비스
 * ============================================================
 *
 * 【역할】
 * Redis에 데이터를 저장/조회/삭제하는 공통 유틸리티입니다.
 * RedisTemplate을 직접 사용하는 대신 이 서비스를 통해 일관된 방식으로 캐시를 관리합니다.
 *
 * 【Redis 키 네이밍 규칙 (이 프로젝트)】
 * - 사용자 프로필:     "user:profile:{userId}"
 * - 채팅방 멤버:       "chatroom:members:{roomId}"
 * - 접속 상태:         "online:users" (SET)
 * - 읽지 않은 메시지:  "chatroom:unread:{roomId}:{userId}"
 *
 * 【TTL (Time To Live)이란?】
 * 캐시 데이터가 자동으로 삭제되는 시간입니다.
 * TTL이 없으면 데이터가 영원히 남아 메모리를 낭비할 수 있습니다.
 * 예: TTL=10분 → 10분 후 자동 삭제 → 다음 조회 시 DB에서 다시 가져와 캐시
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // ===== 기본 String 값 캐싱 =====

    /**
     * 캐시에 값을 저장합니다 (TTL 지정).
     *
     * @param key     Redis 키 (예: "user:profile:42")
     * @param value   저장할 값 (JSON으로 직렬화됨)
     * @param timeout TTL (예: Duration.ofMinutes(10))
     */
    public void set(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
        log.debug("[Redis SET] key={}, TTL={}", key, timeout);
    }

    /**
     * 캐시에서 값을 조회합니다.
     *
     * @param key Redis 키
     * @return 저장된 값 (없으면 null)
     */
    public Object get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        log.debug("[Redis GET] key={}, hit={}", key, value != null);
        return value;
    }

    /**
     * 캐시에서 값을 삭제합니다.
     * 데이터가 변경되었을 때 캐시를 무효화(evict)하는 데 사용합니다.
     *
     * @param key 삭제할 Redis 키
     */
    public void delete(String key) {
        redisTemplate.delete(key);
        log.debug("[Redis DEL] key={}", key);
    }

    /**
     * 특정 키가 존재하는지 확인합니다.
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ===== SET 자료구조 (접속 상태 관리용) =====

    /**
     * SET에 값을 추가합니다. (접속 상태: 온라인 사용자 목록 관리)
     *
     * @param key   SET 키 (예: "online:users")
     * @param value 추가할 값 (예: "user:42")
     */
    public void addToSet(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
        log.debug("[Redis SADD] key={}, value={}", key, value);
    }

    /**
     * SET에서 값을 제거합니다.
     */
    public void removeFromSet(String key, String value) {
        redisTemplate.opsForSet().remove(key, value);
        log.debug("[Redis SREM] key={}, value={}", key, value);
    }

    /**
     * SET의 모든 멤버를 조회합니다.
     */
    public Set<Object> getSetMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * SET에 특정 값이 존재하는지 확인합니다.
     */
    public boolean isSetMember(String key, String value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    // ===== 카운터 (읽지 않은 메시지 수 관리) =====

    /**
     * 카운터를 1 증가시킵니다.
     * 새 메시지가 도착할 때 읽지 않은 메시지 수를 증가시키는 데 사용합니다.
     *
     * @param key 카운터 키 (예: "chatroom:unread:7:42")
     * @return 증가 후 값
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 카운터를 0으로 초기화합니다.
     * 사용자가 채팅방을 읽었을 때 읽지 않은 메시지 수를 초기화합니다.
     */
    public void resetCounter(String key) {
        redisTemplate.opsForValue().set(key, 0);
        log.debug("[Redis RESET] key={}", key);
    }
}
