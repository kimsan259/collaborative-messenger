package com.messenger.user.service;

import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import com.messenger.infrastructure.mail.MailSender;
import com.messenger.user.entity.EmailVerification;
import com.messenger.user.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final MailSender mailSender;

    @Value("${app.email-verification.code-ttl-minutes:10}")
    private long codeTtlMinutes;

    @Value("${app.email-verification.verified-ttl-minutes:30}")
    private long verifiedTtlMinutes;

    @Value("${app.email-verification.debug-expose-code:true}")
    private boolean debugExposeCode;

    @Transactional
    public Map<String, Object> sendCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(codeTtlMinutes);

        EmailVerification entity = EmailVerification.builder()
                .email(email)
                .code(code)
                .expiresAt(expiresAt)
                .verified(false)
                .consumed(false)
                .build();
        emailVerificationRepository.save(entity);

        boolean mailSent = false;
        try {
            mailSender.send(email,
                    "[Narsil] Email verification code",
                    "Your verification code is: " + code + "\nThis code expires in " + codeTtlMinutes + " minutes.");
            mailSent = true;
        } catch (Exception e) {
            log.warn("[email-verification] mail send failed, falling back to code response. email={}, reason={}", email, e.getMessage());
        }

        if (mailSent && !debugExposeCode) {
            return Map.of("email", email, "expiresAt", expiresAt);
        }
        return Map.of("email", email, "expiresAt", expiresAt, "debugCode", code);
    }

    @Transactional
    public void verifyCode(String rawEmail, String rawCode) {
        String email = normalizeEmail(rawEmail);
        String code = rawCode == null ? "" : rawCode.trim();

        EmailVerification latest = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_REQUESTED));

        if (latest.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }
        if (!latest.getCode().equals(code)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_INVALID);
        }
        latest.verify();
    }

    @Transactional
    public void assertAndConsumeVerifiedEmail(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        EmailVerification latest = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED));

        if (!latest.isVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (latest.isConsumed()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (latest.getVerifiedAt() == null || latest.getVerifiedAt().plusMinutes(verifiedTtlMinutes).isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        latest.consume();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
