package com.messenger.infrastructure.mail;

/**
 * 이메일 발송 추상화 인터페이스.
 * SMTP(JavaMailSender) 또는 HTTP API(Resend) 구현체를 교체 가능.
 */
public interface MailSender {
    void send(String to, String subject, String text);
}
