package com.messenger.user.config;

import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPruneInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    @Value("${app.user-maintenance.prune-on-startup:false}")
    private boolean pruneOnStartup;

    @Value("${app.user-maintenance.allow-prune-in-production:false}")
    private boolean allowPruneInProduction;

    @Override
    @Transactional
    public void run(String... args) {
        if (!pruneOnStartup) {
            return;
        }

        if (isProductionProfile() && !allowPruneInProduction) {
            log.warn("[user-prune] blocked in production. set app.user-maintenance.allow-prune-in-production=true to override.");
            return;
        }

        Set<String> whitelist = Set.of("admin", "kimsan4515");
        List<User> allUsers = userRepository.findAll();

        List<User> targets = allUsers.stream()
                .filter(u -> !whitelist.contains(u.getUsername()))
                .collect(Collectors.toList());

        for (User keep : allUsers) {
            if (whitelist.contains(keep.getUsername())) {
                keep.activate();
                keep.markEmailVerified();
            }
        }

        if (targets.isEmpty()) {
            log.info("[user-prune] no users to purge.");
            return;
        }

        List<Long> targetIds = targets.stream().map(User::getId).collect(Collectors.toList());
        List<String> targetEmails = targets.stream()
                .map(User::getEmail)
                .filter(e -> e != null && !e.isBlank())
                .collect(Collectors.toList());

        purgeByUserIds(targetIds, targetEmails);
        log.warn("[user-prune] purged users except whitelist. count={}", targetIds.size());
    }

    private void purgeByUserIds(List<Long> userIds, List<String> emails) {
        String inUsers = placeholders(userIds.size());

        jdbcTemplate.update("DELETE FROM notifications WHERE recipient_id IN (" + inUsers + ")", userIds.toArray());
        jdbcTemplate.update("DELETE FROM friendships WHERE requester_id IN (" + inUsers + ") OR receiver_id IN (" + inUsers + ")",
                concat(userIds, userIds));

        jdbcTemplate.update("DELETE ri FROM report_items ri JOIN daily_reports dr ON ri.daily_report_id = dr.id WHERE dr.user_id IN (" + inUsers + ")", userIds.toArray());
        jdbcTemplate.update("DELETE FROM daily_reports WHERE user_id IN (" + inUsers + ")", userIds.toArray());

        jdbcTemplate.update("DELETE FROM user_teams WHERE user_id IN (" + inUsers + ")", userIds.toArray());
        jdbcTemplate.update("DELETE FROM chat_room_members WHERE user_id IN (" + inUsers + ")", userIds.toArray());

        // chat_messages is sharded by chat_room_id and may exist in both shards; this clears current datasource scope.
        jdbcTemplate.update("DELETE FROM chat_messages WHERE sender_id IN (" + inUsers + ")", userIds.toArray());

        if (!emails.isEmpty()) {
            String inEmails = placeholders(emails.size());
            jdbcTemplate.update("DELETE FROM email_verifications WHERE email IN (" + inEmails + ")", emails.toArray());
        }

        jdbcTemplate.update("DELETE FROM users WHERE id IN (" + inUsers + ")", userIds.toArray());
    }

    private String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private Object[] concat(List<Long> first, List<Long> second) {
        List<Long> merged = new ArrayList<>(first.size() + second.size());
        merged.addAll(first);
        merged.addAll(second);
        return merged.toArray();
    }

    private boolean isProductionProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
