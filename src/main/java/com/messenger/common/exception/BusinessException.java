package com.messenger.common.exception;

import lombok.Getter;

/**
 * ============================================================
 * BusinessException - 비즈니스 로직 전용 예외 클래스
 * ============================================================
 *
 * 【역할】
 * 비즈니스 규칙 위반 시 발생시키는 커스텀 예외입니다.
 * Java의 기본 예외(Exception)와 달리, 우리가 정의한 ErrorCode를 포함합니다.
 *
 * 【왜 커스텀 예외를 만드는가?】
 * 1. 일반 Exception은 에러 원인이 불명확 (어떤 에러인지 코드에서 구분하기 어려움)
 * 2. ErrorCode를 포함하면 에러 종류를 코드로 구분 가능
 * 3. GlobalExceptionHandler에서 이 예외만 잡아서 적절한 HTTP 응답으로 변환 가능
 *
 * 【사용법】
 * Service 클래스에서:
 *   if (user == null) {
 *       throw new BusinessException(ErrorCode.USER_NOT_FOUND);
 *       // → HTTP 404 응답 + "존재하지 않는 사용자입니다." 메시지
 *   }
 *
 * 【동작 흐름】
 * 1. Service에서 throw new BusinessException(ErrorCode.XXX)
 * 2. GlobalExceptionHandler가 이 예외를 자동으로 잡음 (@ExceptionHandler)
 * 3. ErrorCode의 status와 message를 사용하여 ApiResponse 형태로 응답
 * ============================================================
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 이 예외에 대응하는 에러 코드 (에러 종류 + HTTP 상태 코드 + 메시지 포함) */
    private final ErrorCode errorCode;

    /**
     * 에러 코드만으로 예외 생성.
     * ErrorCode에 정의된 기본 메시지가 사용됩니다.
     *
     * @param errorCode 에러 코드 (예: ErrorCode.USER_NOT_FOUND)
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());  // RuntimeException의 메시지에도 설정
        this.errorCode = errorCode;
    }

    /**
     * 에러 코드 + 커스텀 메시지로 예외 생성.
     * ErrorCode의 기본 메시지 대신 직접 지정한 메시지가 사용됩니다.
     *
     * @param errorCode 에러 코드
     * @param message   커스텀 에러 메시지 (예: "사용자 ID 42를 찾을 수 없습니다.")
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
