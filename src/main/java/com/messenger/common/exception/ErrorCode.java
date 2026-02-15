package com.messenger.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT(400, "입력값이 올바르지 않습니다."),

    UNAUTHORIZED(401, "로그인이 필요합니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    LOGIN_FAILED(401, "아이디 또는 비밀번호가 올바르지 않습니다."),

    USER_NOT_FOUND(404, "존재하지 않는 사용자입니다."),
    USERNAME_ALREADY_EXISTS(409, "이미 사용 중인 아이디입니다."),
    EMAIL_ALREADY_EXISTS(409, "이미 사용 중인 이메일입니다."),
    EMAIL_NOT_VERIFIED(400, "이메일 인증이 완료되지 않았습니다."),
    EMAIL_VERIFICATION_NOT_REQUESTED(400, "이메일 인증 코드 요청이 필요합니다."),
    EMAIL_VERIFICATION_INVALID(400, "이메일 인증 코드가 올바르지 않습니다."),
    EMAIL_VERIFICATION_EXPIRED(400, "이메일 인증 코드가 만료되었습니다."),
    EMAIL_VERIFICATION_SEND_FAILED(500, "인증 메일 전송에 실패했습니다."),

    TEAM_NOT_FOUND(404, "존재하지 않는 팀입니다."),
    ALREADY_TEAM_MEMBER(409, "이미 해당 팀의 멤버입니다."),

    CHAT_ROOM_NOT_FOUND(404, "존재하지 않는 채팅방입니다."),
    NOT_CHAT_ROOM_MEMBER(403, "해당 채팅방의 멤버가 아닙니다."),
    CHAT_ROOM_ALREADY_EXISTS(409, "이미 존재하는 채팅방입니다."),

    MESSAGE_NOT_FOUND(404, "존재하지 않는 메시지입니다."),
    MESSAGE_SEND_FAILED(500, "메시지 전송에 실패했습니다."),

    REPORT_NOT_FOUND(404, "존재하지 않는 업무일지입니다."),
    REPORT_ALREADY_EXISTS(409, "해당 날짜의 업무일지가 이미 존재합니다."),
    REPORT_GENERATION_FAILED(500, "업무일지 생성에 실패했습니다."),
    REPORTS_DISABLED(410, "Reports 기능은 현재 비활성화되어 있습니다."),

    ALREADY_FRIENDS(409, "이미 친구 관계입니다."),
    FRIEND_REQUEST_ALREADY_SENT(409, "이미 친구 요청을 보냈습니다."),
    FRIEND_REQUEST_NOT_FOUND(404, "존재하지 않는 친구 요청입니다."),
    CANNOT_ADD_SELF(400, "자기 자신에게 친구 요청을 보낼 수 없습니다."),
    FRIENDSHIP_NOT_FOUND(404, "존재하지 않는 친구 관계입니다."),

    PASSWORD_MISMATCH(400, "현재 비밀번호가 올바르지 않습니다."),

    NOTIFICATION_NOT_FOUND(404, "존재하지 않는 알림입니다.");

    private final int status;
    private final String message;
}
