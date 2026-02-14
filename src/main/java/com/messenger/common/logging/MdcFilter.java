package com.messenger.common.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * ============================================================
 * MdcFilter - 요청 추적 ID(traceId) 자동 부여 필터
 * ============================================================
 *
 * 【역할】
 * 모든 HTTP 요청에 고유한 추적 ID(traceId)를 부여하고,
 * 해당 요청에서 발생하는 모든 로그에 이 ID가 자동으로 포함되게 합니다.
 *
 * 【왜 필요한가? - 실제 디버깅 시나리오】
 * 서버에서 수백 개의 요청이 동시에 처리될 때, 로그가 뒤섞여서
 * 특정 요청의 흐름을 추적하기 어렵습니다.
 *
 * traceId 없이:
 *   14:30:25 메시지 저장 시작
 *   14:30:25 사용자 조회 완료      ← 어떤 요청의 로그인지 알 수 없음
 *   14:30:25 메시지 저장 시작
 *   14:30:25 Kafka 발행 완료
 *
 * traceId 있을 때:
 *   14:30:25 [traceId=a1b2c3d4] 메시지 저장 시작     ← 같은 traceId로 묶어서 추적 가능
 *   14:30:25 [traceId=e5f6g7h8] 사용자 조회 완료
 *   14:30:25 [traceId=a1b2c3d4] Kafka 발행 완료      ← a1b2c3d4 요청의 흐름을 따라갈 수 있음
 *   14:30:25 [traceId=e5f6g7h8] 메시지 저장 시작
 *
 * 【MDC란?】
 * MDC(Mapped Diagnostic Context)는 스레드별 로그 데이터를 저장하는 구조입니다.
 * MDC에 값을 넣으면, 같은 스레드에서 출력하는 모든 로그에 자동으로 포함됩니다.
 * application.yml의 로그 패턴에서 %X{traceId}로 출력합니다.
 *
 * 【동작 순서】
 * 1. HTTP 요청 들어옴
 * 2. traceId 생성 (UUID 앞 8자리) + userId 추출 (세션에서)
 * 3. MDC에 traceId, userId 저장
 * 4. 요청 처리 (이 동안 모든 로그에 traceId가 자동 포함됨)
 * 5. 요청 완료 후 MDC 클리어 (★ 반드시! 스레드풀 재사용 시 데이터 오염 방지)
 * ============================================================
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // 가장 먼저 실행되는 필터 (다른 필터보다 앞서 traceId를 설정)
public class MdcFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            // 1단계: 고유한 추적 ID 생성 (UUID의 앞 8자리만 사용 - 로그 가독성을 위해)
            String traceId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put("traceId", traceId);

            // 2단계: 로그인된 사용자 ID 추출 (세션에서)
            if (request instanceof HttpServletRequest httpRequest) {
                HttpSession session = httpRequest.getSession(false);  // 세션이 없으면 null 반환 (새로 생성하지 않음)
                if (session != null && session.getAttribute("userId") != null) {
                    MDC.put("userId", session.getAttribute("userId").toString());
                } else {
                    MDC.put("userId", "anonymous");  // 비로그인 사용자
                }
            }

            // 3단계: 다음 필터(또는 컨트롤러)로 요청 전달
            chain.doFilter(request, response);

        } finally {
            // 4단계: ★ 반드시 MDC 클리어
            // 톰캣은 스레드풀을 사용하므로, 클리어하지 않으면
            // 다음 요청에서 이전 요청의 traceId/userId가 남아있을 수 있음
            MDC.clear();
        }
    }
}
