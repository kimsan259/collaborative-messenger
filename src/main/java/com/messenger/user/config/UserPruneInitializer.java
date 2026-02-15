package com.messenger.user.config;

import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPruneInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${app.user-maintenance.prune-on-startup:true}")
    private boolean pruneOnStartup;

    @Override
    @Transactional
    public void run(String... args) {
        if (!pruneOnStartup) {
            return;
        }

        Set<String> whitelist = Set.of("admin", "kimsan4515");
        int deactivated = 0;

        for (User user : userRepository.findAll()) {
            if (whitelist.contains(user.getUsername())) {
                user.activate();
                user.markEmailVerified();
                continue;
            }
            if (user.isActive()) {
                user.deactivate();
                deactivated++;
            }
        }

        if (deactivated > 0) {
            log.warn("[user-prune] deactivated users except whitelist. count={}", deactivated);
        } else {
            log.info("[user-prune] no users to deactivate.");
        }
    }
}
