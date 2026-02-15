package com.messenger.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ============================================================
 * UserRegistrationRequest - 회원가입 요청 DTO
 * ============================================================
 *
 * 【DTO란?】
 * Data Transfer Object의 약자로, 클라이언트 ↔ 서버 간 데이터를 전달하는 객체입니다.
 * 엔티티(User)를 직접 노출하지 않고, 필요한 필드만 담아서 전달합니다.
 *
 * 【왜 엔티티를 직접 사용하지 않는가?】
 * 1. 보안: password(해시값)나 내부 필드가 클라이언트에 노출될 수 있음
 * 2. 유연성: API 스펙을 DB 구조와 독립적으로 관리 가능
 * 3. 검증: @NotBlank, @Size 등으로 입력값 검증을 DTO에서 처리
 *
 * 【검증 어노테이션 (@Valid와 함께 동작)】
 * Controller에서 @Valid를 붙이면, 이 DTO의 검증 어노테이션이 자동으로 체크됩니다.
 * 검증 실패 시 → GlobalExceptionHandler의 handleValidationException()이 처리합니다.
 * ============================================================
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {

    /** 로그인 ID (3~50자, 필수) */
    @NotBlank(message = "아이디를 입력해주세요.")
    @Size(min = 3, max = 50, message = "아이디는 3자 이상 50자 이하로 입력해주세요.")
    private String username;

    /** 비밀번호 (4~100자, 필수) */
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 4, max = 100, message = "비밀번호는 4자 이상으로 입력해주세요.")
    private String password;

    /** 표시 이름 (필수) */
    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 100, message = "이름은 100자 이하로 입력해주세요.")
    private String displayName;

    /** 이메일 (필수, 형식 검증) */
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
}
