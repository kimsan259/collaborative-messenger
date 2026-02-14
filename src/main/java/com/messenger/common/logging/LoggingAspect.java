package com.messenger.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * ============================================================
 * LoggingAspect - AOP 기반 자동 메서드 로깅
 * ============================================================
 *
 * 【역할】
 * 모든 Service 클래스의 public 메서드가 실행될 때, 자동으로 다음을 로그에 기록합니다:
 *   - [진입] 메서드 이름 + 파라미터 값
 *   - [완료] 메서드 이름 + 소요 시간 + 반환 값
 *   - [에러] 메서드 이름 + 소요 시간 + 예외 메시지
 *
 * 【왜 AOP를 쓰는가?】
 * 각 Service 메서드마다 로그 코드를 직접 작성하면:
 *   - 코드가 지저분해짐 (비즈니스 로직과 로그 코드가 섞임)
 *   - 로그 형식이 개발자마다 달라짐
 *   - 새 메서드 추가 시 로그 코드를 빠뜨릴 수 있음
 *
 * AOP를 쓰면:
 *   - 비즈니스 코드는 깨끗하게 유지
 *   - 모든 메서드에 일관된 형식의 로그 자동 적용
 *   - 새 메서드 추가 시 별도 작업 불필요
 *
 * 【AOP 용어 간단 설명】
 * - Aspect: 여러 곳에 공통으로 적용할 기능 (이 경우 "로깅")
 * - JoinPoint: 기능이 적용되는 지점 (이 경우 "Service 메서드 실행")
 * - @Around: 메서드 실행 전후를 모두 감싸는 어드바이스
 *
 * 【로그 출력 예시】
 * [진입] ChatMessageService.saveMessage() | 파라미터: [ChatMessageRequest(chatRoomId=7, ...)]
 * [완료] ChatMessageService.saveMessage() | 소요시간: 12ms | 결과: ChatMessage(id=3847)
 * [에러] ChatMessageService.saveMessage() | 소요시간: 5ms | 예외: DB 연결 실패
 * ============================================================
 */
@Slf4j
@Aspect       // 이 클래스가 AOP 기능을 제공하는 Aspect임을 선언
@Component    // Spring Bean으로 등록
public class LoggingAspect {

    /**
     * 【Service 메서드 실행 로깅】
     *
     * @Around: 메서드 실행 전/후를 모두 감쌈
     * execution(* com.messenger..service..*(..)):
     *   - *: 모든 반환 타입
     *   - com.messenger..service: com.messenger 하위의 service 패키지
     *   - ..*: 해당 패키지의 모든 클래스와 메서드
     *   - (..): 모든 파라미터
     *
     * 즉, com.messenger 하위의 모든 service 패키지에 있는
     * 모든 클래스의 모든 메서드에 자동으로 적용됩니다.
     */
    @Around("execution(* com.messenger..service..*(..))")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 실행되는 메서드 이름 (예: "ChatMessageService.saveMessage()")
        String methodName = joinPoint.getSignature().toShortString();

        // 메서드에 전달된 파라미터 값
        Object[] args = joinPoint.getArgs();

        // ===== 메서드 진입 로그 =====
        log.debug("[진입] {} | 파라미터: {}", methodName, Arrays.toString(args));

        // 실행 시간 측정 시작
        long startTime = System.currentTimeMillis();

        try {
            // ===== 실제 메서드 실행 =====
            Object result = joinPoint.proceed();

            // 실행 시간 계산
            long duration = System.currentTimeMillis() - startTime;

            // ===== 메서드 완료 로그 =====
            log.debug("[완료] {} | 소요시간: {}ms", methodName, duration);

            return result;

        } catch (Exception e) {
            // 실행 시간 계산
            long duration = System.currentTimeMillis() - startTime;

            // ===== 메서드 에러 로그 =====
            log.error("[에러] {} | 소요시간: {}ms | 예외: {}", methodName, duration, e.getMessage());

            // 예외를 다시 던져서 정상적인 예외 처리 흐름이 유지되도록 함
            throw e;
        }
    }
}
