package com.messenger.infrastructure.sharding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ============================================================
 * @ShardBy - 샤딩 라우팅 어노테이션
 * ============================================================
 *
 * 【역할】
 * 이 어노테이션이 붙은 메서드의 파라미터에서 chatRoomId를 추출하여
 * 자동으로 ShardKeyHolder에 설정합니다.
 *
 * 【사용법】
 * @ShardBy("chatRoomId")
 * public void saveMessage(Long chatRoomId, ChatMessage message) {
 *     // 이 메서드 내의 DB 쿼리는 chatRoomId 기반으로 자동 라우팅됩니다
 * }
 *
 * 【동작 원리】
 * 1. ShardingAspect(AOP)가 이 어노테이션을 감지
 * 2. value()에 지정된 파라미터 이름으로 chatRoomId 값을 추출
 * 3. ShardKeyHolder.set(chatRoomId) 호출
 * 4. 실제 메서드 실행 (DB 쿼리가 해당 샤드로 라우팅됨)
 * 5. ShardKeyHolder.clear() 호출 (finally 블록에서)
 * ============================================================
 */
@Target(ElementType.METHOD)        // 메서드에만 사용 가능
@Retention(RetentionPolicy.RUNTIME) // 런타임에 AOP가 읽을 수 있도록
public @interface ShardBy {

    /**
     * 샤드 키로 사용할 파라미터 이름.
     * 기본값은 "chatRoomId"입니다.
     */
    String value() default "chatRoomId";
}
