package com.messenger.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ============================================================
 * LoginRequest - 로그인 요청 DTO
 * ============================================================
 *
 * 【역할】
 * 로그인 시 클라이언트가 보내는 아이디/비밀번호를 담는 객체입니다.
 * ============================================================
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}
