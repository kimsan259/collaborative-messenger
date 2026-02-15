package com.messenger.infrastructure.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Resend.com HTTP API를 사용한 이메일 발송.
 * SMTP 포트가 차단된 Railway 등 PaaS 환경에서 사용.
 *
 * 활성화 조건: app.mail.provider=resend (환경변수 APP_MAIL_PROVIDER=resend)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendMailSender implements MailSender {

    private final RestClient restClient;
    private final String from;

    public ResendMailSender(
            @Value("${app.mail.resend-api-key:}") String apiKey,
            @Value("${app.mail.from:onboarding@resend.dev}") String from) {
        this.from = from;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        log.info("[ResendMailSender] initialized. from={}", from);
    }

    @Override
    public void send(String to, String subject, String text) {
        try {
            String response = restClient.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", from,
                            "to", new String[]{to},
                            "subject", subject,
                            "text", text
                    ))
                    .retrieve()
                    .body(String.class);
            log.info("[ResendMailSender] sent to={}, response={}", to, response);
        } catch (Exception e) {
            log.error("[ResendMailSender] failed to={}, error={}", to, e.getMessage());
            throw new RuntimeException("Resend mail send failed: " + e.getMessage(), e);
        }
    }
}
