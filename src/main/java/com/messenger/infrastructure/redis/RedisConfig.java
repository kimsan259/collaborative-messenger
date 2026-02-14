package com.messenger.infrastructure.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * ============================================================
 * RedisConfig - Redis 연결 및 직렬화 설정
 * ============================================================
 *
 * 【역할】
 * Redis 서버 연결 설정과 데이터 직렬화 방식을 정의합니다.
 *
 * 【Redis 사용처 (이 프로젝트)】
 * 1. 세션 저장 (Spring Session): 사용자 로그인 상태 유지
 * 2. 캐시: 사용자 프로필, 채팅방 멤버 목록 등 자주 조회하는 데이터
 * 3. 접속 상태: 사용자의 온라인/오프라인 상태 (SET 자료구조)
 * 4. 읽지 않은 메시지 수: 채팅방별 안 읽은 메시지 카운터
 *
 * 【직렬화란?】
 * Java 객체를 Redis에 저장하려면 바이트 배열로 변환해야 합니다.
 * - Key: StringRedisSerializer (문자열 그대로 저장)
 * - Value: StringRedisSerializer (문자열로 저장 → 디버깅 편리)
 *
 * 문자열로 저장하면 redis-cli에서 사람이 읽을 수 있어 디버깅이 편합니다.
 * ============================================================
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    /**
     * 【Redis 연결 팩토리】
     * Redis 서버와의 연결을 관리합니다.
     * Lettuce: 비동기/논블로킹 Redis 클라이언트 (Spring Boot 기본)
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setPassword(password);
        return new LettuceConnectionFactory(config);
    }

    /**
     * 【RedisTemplate】
     * Redis에 데이터를 저장/조회할 때 사용하는 핵심 객체입니다.
     *
     * 사용법:
     *   redisTemplate.opsForValue().set("user:1", "홍길동");              // 문자열 저장
     *   redisTemplate.opsForValue().get("user:1");                        // 문자열 조회
     *   redisTemplate.opsForSet().add("online:users", "user:1");         // Set에 추가
     *   redisTemplate.opsForHash().put("room:1", "name", "개발팀");      // Hash에 저장
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key/Value 모두 문자열로 직렬화 (redis-cli에서 읽기 편리)
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
