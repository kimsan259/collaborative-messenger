package com.messenger.infrastructure.security;

import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ============================================================
 * CustomUserDetailsService - Spring Security용 사용자 조회 서비스
 * ============================================================
 *
 * 【역할】
 * Spring Security가 인증(로그인)할 때 DB에서 사용자 정보를 가져오는 역할입니다.
 *
 * 【왜 필요한가?】
 * Spring Security는 로그인 시 UserDetailsService.loadUserByUsername()을 호출합니다.
 * 이 클래스가 없으면 Spring이 기본 inMemoryUserDetailsManager를 사용하여
 * DB에 저장된 사용자를 인식하지 못합니다.
 *
 * 【동작 흐름】
 * 1. 사용자가 로그인 요청 → AuthService.login() 호출
 * 2. AuthService에서 SecurityContext에 인증 정보 설정
 * 3. Spring Security가 이 클래스를 통해 사용자 정보 확인
 * 4. 인증 성공 → 세션에 인증 정보 저장 → 이후 요청에서 인증 상태 유지
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 【username으로 사용자 정보를 로드합니다】
     *
     * Spring Security가 인증 시 자동으로 호출하는 메서드입니다.
     * DB에서 사용자를 조회하고, Spring Security가 이해할 수 있는
     * UserDetails 객체로 변환하여 반환합니다.
     *
     * @param username 로그인 ID
     * @return UserDetails (Spring Security가 사용하는 사용자 정보 객체)
     * @throws UsernameNotFoundException 사용자가 없을 때
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("[Spring Security] 사용자 조회: username={}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[Spring Security] 사용자를 찾을 수 없음: username={}", username);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
                });

        // User 엔티티 → Spring Security의 UserDetails로 변환
        // ROLE_ 접두사: Spring Security의 권한 규약 (ROLE_ADMIN, ROLE_MEMBER 등)
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}
