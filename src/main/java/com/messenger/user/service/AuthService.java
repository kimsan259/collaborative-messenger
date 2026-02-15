package com.messenger.user.service;

import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import com.messenger.user.dto.LoginRequest;
import com.messenger.user.dto.UserResponse;
import com.messenger.user.entity.User;
import com.messenger.user.entity.UserStatus;
import com.messenger.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse login(LoginRequest request, HttpSession session) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("displayName", user.getDisplayName());
        session.setAttribute("role", user.getRole().name());

        user.updateStatus(UserStatus.ONLINE);
        log.info("[login] userId={}, username={}, sessionId={}", user.getId(), user.getUsername(), session.getId());
        return UserResponse.from(user);
    }

    @Transactional
    public void logout(HttpSession session) {
        String username = (String) session.getAttribute("username");
        Long userId = (Long) session.getAttribute("userId");

        if (userId != null) {
            userRepository.findById(userId).ifPresent(u -> u.updateStatus(UserStatus.OFFLINE));
        }

        log.info("[logout] username={}, sessionId={}", username, session.getId());
        session.invalidate();
    }
}
