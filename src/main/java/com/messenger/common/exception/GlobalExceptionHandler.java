package com.messenger.common.exception;

import com.messenger.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * ============================================================
 * GlobalExceptionHandler - 전역 예외 처리기
 * ============================================================
 *
 * 【역할】
 * 애플리케이션 전체에서 발생하는 예외를 한 곳에서 처리합니다.
 * Controller마다 try-catch를 작성할 필요 없이, 여기서 모든 예외를 잡아서
 * 클라이언트에게 일관된 형식(ApiResponse)으로 에러 응답을 보냅니다.
 *
 * 【동작 원리】
 * 1. @RestControllerAdvice: 모든 Controller에서 발생하는 예외를 가로챔
 * 2. @ExceptionHandler: 특정 예외 타입별로 처리 메서드를 지정
 * 3. 예외 발생 → 이 클래스의 해당 핸들러 메서드가 호출 → ApiResponse로 변환 → 클라이언트에 응답
 *
 * 【처리하는 예외 종류】
 * - BusinessException: 우리가 의도적으로 던진 비즈니스 에러 (예: 사용자 없음)
 * - MethodArgumentNotValidException: @Valid 검증 실패 시 (예: 비밀번호가 빈 값)
 * - Exception: 예상치 못한 서버 에러 (NullPointer 등)
 * ============================================================
 */
@Slf4j                    // Lombok: log 변수 자동 생성 (log.error(), log.warn() 사용 가능)
@RestControllerAdvice     // 모든 Controller에서 발생하는 예외를 잡는 전역 핸들러
public class GlobalExceptionHandler {

    /**
     * 【비즈니스 예외 처리】
     * BusinessException이 발생하면 이 메서드가 호출됩니다.
     * ErrorCode에 정의된 HTTP 상태 코드와 메시지로 응답합니다.
     *
     * 예시: throw new BusinessException(ErrorCode.USER_NOT_FOUND)
     *       → HTTP 404 + {"success": false, "message": "존재하지 않는 사용자입니다."}
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        // WARN 레벨: 비즈니스 예외는 예상된 에러이므로 ERROR가 아닌 WARN으로 기록
        log.warn("[비즈니스 예외] 에러코드={}, 메시지={}", e.getErrorCode().name(), e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getStatus())   // ErrorCode의 HTTP 상태 코드 사용
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 【입력값 검증 실패 처리】
     * @Valid 어노테이션으로 DTO를 검증할 때 실패하면 이 메서드가 호출됩니다.
     * 어떤 필드가 어떤 이유로 실패했는지 상세 메시지를 생성합니다.
     *
     * 예시: @NotBlank가 붙은 username에 빈 값이 들어옴
     *       → HTTP 400 + {"success": false, "message": "username: 아이디를 입력해주세요."}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        // 검증 실패한 모든 필드의 에러 메시지를 한 줄로 합침
        // 예: "username: 아이디를 입력해주세요., password: 비밀번호를 입력해주세요."
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("[입력값 검증 실패] {}", errorMessage);

        return ResponseEntity
                .badRequest()   // HTTP 400
                .body(ApiResponse.error(errorMessage));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("[정적 리소스 없음] {}", e.getMessage());
        return ResponseEntity
                .status(404)
                .body(ApiResponse.error("요청한 리소스를 찾을 수 없습니다."));
    }

    /**
     * 【예상치 못한 서버 에러 처리】
     * 위에서 잡지 못한 모든 예외가 여기로 들어옵니다.
     * NullPointerException, DB 연결 실패 등 예상하지 못한 에러입니다.
     *
     * ★ ERROR 레벨로 로그를 남기고, 스택 트레이스도 함께 기록합니다.
     *   운영 환경에서 이 로그가 보이면 즉시 확인이 필요합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // ERROR 레벨 + 스택 트레이스 포함: 원인 파악을 위해 상세 로그 기록
        log.error("[서버 에러] 예상치 못한 오류 발생", e);

        return ResponseEntity
                .internalServerError()  // HTTP 500
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다. 관리자에게 문의해주세요."));
    }
}
