package com.messenger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 통합 테스트: MySQL, Redis, Kafka 등 인프라가 실행 중일 때만 동작합니다.
 * docker compose up 실행 후 이 테스트를 활성화하세요.
 */
@SpringBootTest
@Disabled("인프라(MySQL/Redis/Kafka)가 실행 중일 때만 활성화")
class CollaborativeMessengerApplicationTests {

	@Test
	void contextLoads() {
	}

}
