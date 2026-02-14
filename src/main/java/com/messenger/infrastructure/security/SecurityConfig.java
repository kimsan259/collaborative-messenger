package com.messenger.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ============================================================
 * SecurityConfig - Spring Security 보안 설정
 * ============================================================
 *
 * 【역할】
 * HTTP 요청에 대한 인증(Authentication)과 인가(Authorization)를 설정합니다.
 *
 * 【인증 vs 인가】
 * - 인증(Authentication): "너 누구야?" → 로그인으로 확인
 * - 인가(Authorization):  "너 이거 할 수 있어?" → 권한으로 확인
 *
 * 【이 프로젝트의 보안 정책】
 * 1. 세션 기반 인증 (JWT 아닌 HttpSession)
 * 2. 회원가입/로그인 페이지는 누구나 접근 가능 (permitAll)
 * 3. 정적 자원(CSS, JS, 이미지)은 누구나 접근 가능
 * 4. 나머지 모든 요청은 로그인 필요 (authenticated)
 * 5. WebSocket 엔드포인트는 별도 인증 처리 (WebSocketAuthInterceptor)
 *
 * 【CSRF 비활성화 이유】
 * REST API에서는 CSRF 토큰 관리가 복잡하고,
 * 세션 쿠키에 SameSite 속성이 설정되어 있으므로 CSRF 공격 위험이 낮습니다.
 * ★ 실무에서는 CSRF 활성화를 검토해야 합니다.
 * ============================================================
 */
@Configuration
@EnableWebSecurity  // Spring Security 활성화
public class SecurityConfig {

    /**
     * 【비밀번호 암호화 도구】
     * BCrypt: 업계 표준 비밀번호 해시 알고리즘
     * - 같은 비밀번호라도 매번 다른 해시값 생성 (salt 내장)
     * - 레인보우 테이블 공격 방지
     * - 연산 비용이 높아 무차별 대입(brute force) 공격에 강함
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 【HTTP 보안 설정】
     * 어떤 URL에 누가 접근할 수 있는지 규칙을 정의합니다.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ===== CSRF 비활성화 (REST API 호환을 위해) =====
            .csrf(csrf -> csrf.disable())

            // ===== URL별 접근 권한 설정 =====
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능한 경로들
                .requestMatchers(
                    "/auth/**",            // 로그인/회원가입 페이지
                    "/api/auth/**",        // 인증 API (로그인, 회원가입)
                    "/ws/**",              // WebSocket 연결 엔드포인트
                    "/css/**",             // CSS 파일
                    "/js/**",              // JavaScript 파일
                    "/webjars/**",         // WebJars (jQuery, Bootstrap 등)
                    "/uploads/**",         // 업로드된 파일 (프로필 이미지)
                    "/error"               // 에러 페이지
                ).permitAll()

                // 그 외 모든 요청은 로그인 필요
                .anyRequest().authenticated()
            )

            // ===== 로그인 설정 =====
            // ★ 커스텀 AJAX 로그인을 사용하므로 formLogin은 로그인 페이지 지정만 합니다.
            //    실제 인증 처리는 AuthService.login()에서 SecurityContext를 직접 설정합니다.
            .formLogin(form -> form
                .loginPage("/auth/login")           // 미인증 시 리다이렉트할 로그인 페이지
                .loginProcessingUrl("/auth/form-login-unused") // 사용하지 않는 URL (AJAX 방식 사용)
                .defaultSuccessUrl("/chat/rooms")   // 로그인 성공 시 이동할 페이지
                .permitAll()
            )

            // ===== 로그아웃 설정 =====
            .logout(logout -> logout
                .logoutUrl("/auth/logout")          // 로그아웃 URL
                .logoutSuccessUrl("/auth/login")    // 로그아웃 후 이동할 페이지
                .invalidateHttpSession(true)        // 세션 무효화
                .deleteCookies("JSESSIONID")        // 쿠키 삭제
            );

        return http.build();
    }
}
