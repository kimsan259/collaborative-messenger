package com.messenger.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * ============================================================
 * ApiResponse - 통일된 API 응답 형식
 * ============================================================
 *
 * 【역할】
 * 모든 REST API의 응답을 동일한 형식으로 감싸는 래퍼(wrapper) 클래스입니다.
 * 프론트엔드에서 항상 같은 구조로 응답을 처리할 수 있게 해줍니다.
 *
 * 【응답 형식 예시】
 * 성공 시:
 * {
 *   "success": true,
 *   "message": "사용자 정보를 조회했습니다.",
 *   "data": { "id": 1, "username": "kim", ... }
 * }
 *
 * 실패 시:
 * {
 *   "success": false,
 *   "message": "존재하지 않는 사용자입니다.",
 *   "data": null    ← null인 필드는 응답에서 제외됨 (@JsonInclude)
 * }
 *
 * 【사용법】
 * Controller에서:
 *   return ApiResponse.success("조회 성공", userData);     // 성공 응답
 *   return ApiResponse.success("삭제 완료");               // 데이터 없는 성공 응답
 *   return ApiResponse.error("사용자를 찾을 수 없습니다"); // 에러 응답
 *
 * 【제네릭 <T>란?】
 * T는 data 필드에 담길 데이터의 타입을 의미합니다.
 * 예를 들어 ApiResponse<UserResponse>면 data에 UserResponse 객체가 들어갑니다.
 * ============================================================
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 응답에서 제외 (깔끔한 응답)
public class ApiResponse<T> {

    /** 요청 성공 여부 (true: 성공, false: 실패) */
    private final boolean success;

    /** 사용자에게 보여줄 메시지 */
    private final String message;

    /** 실제 응답 데이터 (성공 시에만 값이 있음) */
    private final T data;

    /**
     * 성공 응답 생성 (데이터 포함)
     *
     * @param message 성공 메시지 (예: "사용자 정보를 조회했습니다.")
     * @param data    응답 데이터
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     *
     * @param message 성공 메시지 (예: "삭제가 완료되었습니다.")
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * 에러 응답 생성
     *
     * @param message 에러 메시지 (예: "존재하지 않는 사용자입니다.")
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
