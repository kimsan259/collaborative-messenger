package com.messenger.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ============================================================
 * ErrorCode - 에러 코드 통합 관리
 * ============================================================
 *
 * 【역할】
 * 애플리케이션에서 발생할 수 있는 모든 에러를 한 곳에서 관리합니다.
 * 에러 코드를 enum으로 관리하면 다음과 같은 장점이 있습니다:
 *   1. 에러 메시지가 코드 전체에서 일관됨
 *   2. 새로운 에러 추가 시 여기에만 추가하면 됨
 *   3. HTTP 상태 코드와 에러 메시지를 짝으로 관리
 *
 * 【HTTP 상태 코드 참고】
 *   400 Bad Request    : 클라이언트가 잘못된 요청을 보냄 (입력값 오류)
 *   401 Unauthorized   : 로그인이 필요함 (인증 실패)
 *   403 Forbidden      : 권한이 없음 (인가 실패)
 *   404 Not Found      : 요청한 자원을 찾을 수 없음
 *   409 Conflict       : 중복 데이터 등 충돌 발생
 *   500 Internal Error : 서버 내부 오류
 *
 * 【사용법】
 * throw new BusinessException(ErrorCode.USER_NOT_FOUND);
 * ============================================================
 */
@Getter
@RequiredArgsConstructor  // Lombok: final 필드를 매개변수로 받는 생성자 자동 생성
public enum ErrorCode {

    // ===== 공통 에러 =====
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT(400, "입력값이 올바르지 않습니다."),

    // ===== 인증/인가 에러 =====
    UNAUTHORIZED(401, "로그인이 필요합니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    LOGIN_FAILED(401, "아이디 또는 비밀번호가 올바르지 않습니다."),

    // ===== 사용자 에러 =====
    USER_NOT_FOUND(404, "존재하지 않는 사용자입니다."),
    USERNAME_ALREADY_EXISTS(409, "이미 사용 중인 아이디입니다."),
    EMAIL_ALREADY_EXISTS(409, "이미 사용 중인 이메일입니다."),
    EMAIL_NOT_VERIFIED(400, "이메일 인증이 완료되지 않았습니다."),
    EMAIL_VERIFICATION_NOT_REQUESTED(400, "이메일 인증 코드 요청이 필요합니다."),
    EMAIL_VERIFICATION_INVALID(400, "이메일 인증 코드가 올바르지 않습니다."),
    EMAIL_VERIFICATION_EXPIRED(400, "이메일 인증 코드가 만료되었습니다."),
    EMAIL_VERIFICATION_SEND_FAILED(500, "인증 메일 전송에 실패했습니다."),

    // ===== 팀 에러 =====
    TEAM_NOT_FOUND(404, "존재하지 않는 팀입니다."),
    ALREADY_TEAM_MEMBER(409, "이미 해당 팀의 멤버입니다."),

    // ===== 채팅방 에러 =====
    CHAT_ROOM_NOT_FOUND(404, "존재하지 않는 채팅방입니다."),
    NOT_CHAT_ROOM_MEMBER(403, "해당 채팅방의 멤버가 아닙니다."),
    CHAT_ROOM_ALREADY_EXISTS(409, "이미 존재하는 채팅방입니다."),

    // ===== 메시지 에러 =====
    MESSAGE_NOT_FOUND(404, "존재하지 않는 메시지입니다."),
    MESSAGE_SEND_FAILED(500, "메시지 전송에 실패했습니다."),

    // ===== 업무일지 에러 =====
    REPORT_NOT_FOUND(404, "존재하지 않는 업무일지입니다."),
    REPORT_ALREADY_EXISTS(409, "해당 날짜의 업무일지가 이미 존재합니다."),
    REPORT_GENERATION_FAILED(500, "업무일지 생성에 실패했습니다."),
    REPORTS_DISABLED(410, "Reports 기능은 현재 비활성화되어 있습니다."),

    // ===== 친구 에러 =====
    ALREADY_FRIENDS(409, "이미 친구 관계입니다."),
    FRIEND_REQUEST_ALREADY_SENT(409, "이미 친구 요청을 보냈습니다."),
    FRIEND_REQUEST_NOT_FOUND(404, "존재하지 않는 친구 요청입니다."),
    CANNOT_ADD_SELF(400, "자기 자신에게 친구 요청을 보낼 수 없습니다."),
    FRIENDSHIP_NOT_FOUND(404, "존재하지 않는 친구 관계입니다."),

    // ===== 프로필 에러 =====
    PASSWORD_MISMATCH(400, "현재 비밀번호가 올바르지 않습니다."),

    // ===== 알림 에러 =====
    NOTIFICATION_NOT_FOUND(404, "존재하지 않는 알림입니다.");

    /** HTTP 상태 코드 (예: 404) */
    private final int status;

    /** 사용자에게 보여줄 에러 메시지 (예: "존재하지 않는 사용자입니다.") */
    private final String message;
}
