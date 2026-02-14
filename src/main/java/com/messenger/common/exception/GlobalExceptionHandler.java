package com.messenger.common.exception;

import com.messenger.common.debug.RecentErrorLogService;
import com.messenger.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final RecentErrorLogService recentErrorLogService;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("[business-exception] code={}, message={}", e.getErrorCode().name(), e.getMessage());
        recentErrorLogService.capture("business", e.getMessage(), e.getErrorCode().name());

        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("[validation-failed] {}", errorMessage);
        recentErrorLogService.capture("validation", errorMessage, "MethodArgumentNotValidException");

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(errorMessage));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("[resource-not-found] {}", e.getMessage());
        recentErrorLogService.capture("resource-not-found", e.getMessage(), "NoResourceFoundException");
        return ResponseEntity
                .status(404)
                .body(ApiResponse.error("요청한 리소스를 찾을 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[server-error] unexpected error", e);
        recentErrorLogService.capture("exception", e.getMessage(), e.getClass().getSimpleName());

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다. 관리자에게 문의해주세요."));
    }
}
