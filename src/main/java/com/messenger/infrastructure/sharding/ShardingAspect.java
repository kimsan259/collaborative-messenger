package com.messenger.infrastructure.sharding;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;

/**
 * ============================================================
 * ShardingAspect - AOP 기반 자동 샤드 라우팅
 * ============================================================
 *
 * 【역할】
 * @ShardBy 어노테이션이 붙은 메서드가 실행될 때,
 * 자동으로 ShardKeyHolder에 샤드 키를 설정하고 해제합니다.
 *
 * 【왜 AOP를 사용하는가?】
 * 모든 샤딩 관련 메서드에 수동으로 ShardKeyHolder.set() / clear()를
 * 작성하면 코드가 반복되고, clear()를 빠뜨릴 위험이 있습니다.
 *
 * AOP를 사용하면:
 *   - @ShardBy만 붙이면 자동으로 샤드 라우팅 처리
 *   - finally 블록에서 반드시 clear() 호출 (누락 방지)
 *   - 비즈니스 코드가 샤딩 로직과 분리되어 깔끔함
 *
 * 【동작 예시】
 *
 * @ShardBy("chatRoomId")
 * public void saveMessage(Long chatRoomId, ChatMessage msg) { ... }
 *
 * 호출: saveMessage(7, message)
 *
 * AOP 처리:
 * 1. 파라미터에서 "chatRoomId" = 7 추출
 * 2. ShardKeyHolder.set(7)
 * 3. saveMessage() 실행 → DB 쿼리가 shard_1로 라우팅 (7%2=1)
 * 4. ShardKeyHolder.clear()
 * ============================================================
 */
@Slf4j
@Aspect
@Component
public class ShardingAspect {

    /**
     * @ShardBy 어노테이션이 붙은 메서드 실행 전후를 감쌉니다.
     *
     * @Around: 메서드 실행 전에 샤드 키를 설정하고, 실행 후에 해제
     * @annotation(shardBy): @ShardBy 어노테이션이 붙은 메서드에만 적용
     */
    @Around("@annotation(shardBy)")
    public Object routeShard(ProceedingJoinPoint joinPoint, ShardBy shardBy) throws Throwable {
        // 1단계: 어노테이션에 지정된 파라미터 이름으로 값 추출
        String paramName = shardBy.value();  // 기본값: "chatRoomId"
        Long shardKey = extractShardKey(joinPoint, paramName);

        if (shardKey != null) {
            log.debug("[ShardingAspect] 메서드={}, 파라미터={}={}, 샤드=shard_{}",
                    joinPoint.getSignature().toShortString(), paramName, shardKey, shardKey % 2);
        }

        try {
            // 2단계: 샤드 키 설정
            if (shardKey != null) {
                ShardKeyHolder.set(shardKey);
            }

            // 3단계: 실제 메서드 실행
            return joinPoint.proceed();

        } finally {
            // 4단계: ★ 반드시 샤드 키 해제 (메모리 누수 + 잘못된 라우팅 방지)
            ShardKeyHolder.clear();
        }
    }

    /**
     * 메서드 파라미터에서 샤드 키 값을 추출합니다.
     *
     * @param joinPoint AOP 조인포인트 (실행 중인 메서드 정보)
     * @param paramName 추출할 파라미터 이름 (예: "chatRoomId")
     * @return 파라미터 값 (Long), 찾지 못하면 null
     */
    private Long extractShardKey(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        // 모든 파라미터를 순회하며 이름이 일치하는 것을 찾음
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName) && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }

        // 파라미터 이름 매칭 실패 시, 첫 번째 Long 파라미터를 사용 (폴백)
        for (Object arg : args) {
            if (arg instanceof Long) {
                log.debug("[ShardingAspect] 파라미터 이름 '{}' 미발견, 첫 번째 Long 값 사용", paramName);
                return (Long) arg;
            }
        }

        log.warn("[ShardingAspect] 샤드 키를 찾을 수 없음 - 기본 샤드(shard_0) 사용");
        return null;
    }
}
