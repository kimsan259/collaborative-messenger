package com.messenger.infrastructure.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 이메일 미설정 시 사용되는 기본 구현체.
 * 실제 메일을 보내지 않고 로그만 남김.
 *
 * 활성화 조건: app.mail.provider=none (기본값)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "none", matchIfMissing = true)
public class SmtpMailSender implements MailSender {

    @Override
    public void send(String to, String subject, String text) {
        log.warn("[NoopMailSender] mail provider not configured. to={}, subject={}", to, subject);
        throw new IllegalStateException("Mail provider is not configured. Set APP_MAIL_PROVIDER=resend");
    }
}
