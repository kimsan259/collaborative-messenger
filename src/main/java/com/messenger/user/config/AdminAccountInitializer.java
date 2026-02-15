package com.messenger.user.config;

import com.messenger.user.entity.User;
import com.messenger.user.entity.UserRole;
import com.messenger.user.entity.UserStatus;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        String adminUsername = "admin";

        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }

        User admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode("admin"))
                .displayName("Administrator")
                .email("admin@local")
                .status(UserStatus.OFFLINE)
                .role(UserRole.ADMIN)
                .active(true)
                .emailVerified(true)
                .build();

        userRepository.save(admin);
        log.warn("[admin-init] default admin account created. username=admin");
    }
}

