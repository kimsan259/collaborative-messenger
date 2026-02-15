package com.messenger.user.controller;

import com.messenger.common.dto.ApiResponse;
import com.messenger.user.dto.EmailSendRequest;
import com.messenger.user.dto.EmailVerifyRequest;
import com.messenger.user.dto.LoginRequest;
import com.messenger.user.dto.UserRegistrationRequest;
import com.messenger.user.dto.UserResponse;
import com.messenger.user.service.AuthService;
import com.messenger.user.service.EmailVerificationService;
import com.messenger.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    @GetMapping("/auth/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/auth/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserRegistrationRequest request) {

        UserResponse user = userService.register(request);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", user));
    }

    @PostMapping("/api/auth/email/send")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> sendEmailVerificationCode(
            @Valid @RequestBody EmailSendRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                "이메일 인증 코드를 전송했습니다.",
                emailVerificationService.sendCode(request.getEmail())
        ));
    }

    @PostMapping("/api/auth/email/verify")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> verifyEmailCode(
            @Valid @RequestBody EmailVerifyRequest request) {

        emailVerificationService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다."));
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session) {

        UserResponse user = authService.login(request, session);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", user));
    }

    @PostMapping("/api/auth/logout")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.ok(ApiResponse.success("로그아웃했습니다."));
    }
}
