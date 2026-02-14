package com.messenger.user.controller;

import com.messenger.common.dto.ApiResponse;
import com.messenger.user.dto.LoginRequest;
import com.messenger.user.dto.UserRegistrationRequest;
import com.messenger.user.dto.UserResponse;
import com.messenger.user.service.AuthService;
import com.messenger.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================
 * AuthController - 인증(회원가입/로그인/로그아웃) 컨트롤러
 * ============================================================
 *
 * 【역할】
 * 인증 관련 HTTP 요청을 받아서 적절한 Service로 전달하고, 응답을 반환합니다.
 *
 * 【엔드포인트 목록】
 * GET  /auth/login     → 로그인 페이지 표시
 * GET  /auth/register  → 회원가입 페이지 표시
 * POST /api/auth/register → 회원가입 처리 (REST API)
 * POST /api/auth/login    → 로그인 처리 (REST API)
 * POST /api/auth/logout   → 로그아웃 처리 (REST API)
 *
 * 【Controller vs RestController】
 * - @Controller: HTML 페이지를 반환할 수 있음 (Thymeleaf 템플릿)
 * - @RestController: JSON만 반환 (API 전용)
 * - 여기서는 @Controller를 사용하여 페이지 + API 모두 처리
 *   (@ResponseBody를 붙이면 JSON 반환)
 * ============================================================
 */
@Slf4j
@Controller             // HTML 페이지도 반환 가능한 컨트롤러
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    // ===== 페이지 요청 (Thymeleaf 템플릿 반환) =====

    /** 로그인 페이지를 표시합니다. */
    @GetMapping("/auth/login")
    public String loginPage() {
        return "login";  // src/main/resources/templates/login.html
    }

    /** 회원가입 페이지를 표시합니다. */
    @GetMapping("/auth/register")
    public String registerPage() {
        return "register";  // src/main/resources/templates/register.html
    }

    // ===== REST API (JSON 반환) =====

    /**
     * 【회원가입 API】
     * POST /api/auth/register
     *
     * @param request 회원가입 요청 데이터 (JSON)
     * @return 생성된 사용자 정보
     *
     * @Valid: request의 검증 어노테이션(@NotBlank, @Size 등)을 자동 체크
     *         검증 실패 시 GlobalExceptionHandler가 처리
     */
    @PostMapping("/api/auth/register")
    @ResponseBody  // JSON 응답
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserRegistrationRequest request) {

        UserResponse user = userService.register(request);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", user));
    }

    /**
     * 【로그인 API】
     * POST /api/auth/login
     *
     * @param request 로그인 요청 데이터 (username, password)
     * @param session HTTP 세션 (Spring이 자동 주입)
     * @return 로그인된 사용자 정보
     */
    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session) {

        UserResponse user = authService.login(request, session);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", user));
    }

    /**
     * 【로그아웃 API】
     * POST /api/auth/logout
     */
    @PostMapping("/api/auth/logout")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다."));
    }
}
