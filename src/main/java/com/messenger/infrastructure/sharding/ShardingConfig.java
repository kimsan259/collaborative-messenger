package com.messenger.infrastructure.sharding;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * ShardingConfig - MySQL 샤딩 DataSource 설정
 * ============================================================
 *
 * 【역할】
 * 2개의 MySQL 인스턴스(shard_0, shard_1)를 DataSource로 등록하고,
 * ShardingDataSourceRouter로 동적 라우팅을 설정합니다.
 *
 * 【구성도】
 *
 *                    ┌── shard_0 (MySQL 3307) ── 모든 테이블 + 짝수 메시지
 * [Application] ──→  │
 *     (Router)       └── shard_1 (MySQL 3308) ── 홀수 메시지만
 *
 * 【HikariCP란?】
 * 데이터베이스 커넥션 풀 라이브러리입니다.
 * DB 연결을 미리 만들어 놓고 재사용하여 성능을 최적화합니다.
 * Spring Boot의 기본 커넥션 풀이며, 업계 최고 성능을 자랑합니다.
 *
 * 【주의: Spring Boot 자동 설정과의 충돌】
 * 우리가 직접 DataSource를 구성하므로, Spring Boot의
 * spring.datasource 자동 설정은 사용하지 않습니다.
 * application.yml에서 커스텀 프로퍼티(datasource.shard0, shard1)를 사용합니다.
 * ============================================================
 */
@Slf4j
@Configuration
public class ShardingConfig {

    // ===== Shard 0 설정값 (application.yml에서 읽음) =====
    @Value("${datasource.shard0.url}")
    private String shard0Url;
    @Value("${datasource.shard0.username}")
    private String shard0Username;
    @Value("${datasource.shard0.password}")
    private String shard0Password;
    @Value("${datasource.shard0.driver-class-name}")
    private String shard0Driver;

    // ===== Shard 1 설정값 =====
    @Value("${datasource.shard1.url}")
    private String shard1Url;
    @Value("${datasource.shard1.username}")
    private String shard1Username;
    @Value("${datasource.shard1.password}")
    private String shard1Password;
    @Value("${datasource.shard1.driver-class-name}")
    private String shard1Driver;

    /**
     * 【Shard 0 DataSource 생성】
     * MySQL 포트 3307에 연결하는 커넥션 풀입니다.
     * 모든 테이블 + 짝수 chatRoomId의 메시지가 저장됩니다.
     */
    private DataSource createShard0DataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(shard0Url);
        ds.setUsername(shard0Username);
        ds.setPassword(shard0Password);
        ds.setDriverClassName(shard0Driver);
        ds.setPoolName("shard-0-pool");        // 커넥션 풀 이름 (모니터링용)
        ds.setMaximumPoolSize(10);              // 최대 커넥션 수
        ds.setMinimumIdle(5);                   // 최소 유휴 커넥션 수
        return ds;
    }

    /**
     * 【Shard 1 DataSource 생성】
     * MySQL 포트 3308에 연결하는 커넥션 풀입니다.
     * 홀수 chatRoomId의 메시지만 저장됩니다.
     */
    private DataSource createShard1DataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(shard1Url);
        ds.setUsername(shard1Username);
        ds.setPassword(shard1Password);
        ds.setDriverClassName(shard1Driver);
        ds.setPoolName("shard-1-pool");
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(5);
        return ds;
    }

    /**
     * 【라우팅 DataSource (메인 DataSource)】
     *
     * @Primary: Spring이 DataSource를 필요로 할 때 이 빈을 우선 사용
     *
     * 동작:
     * 1. targetDataSources에 "shard_0"과 "shard_1"을 등록
     * 2. defaultTargetDataSource를 shard_0으로 설정 (기본값)
     * 3. ShardKeyHolder에 키가 설정되어 있으면 해당 샤드로 라우팅
     * 4. 키가 없으면 기본 샤드(shard_0)로 라우팅
     */
    @Bean
    @Primary  // JPA가 사용할 기본 DataSource
    public DataSource dataSource() {
        ShardingDataSourceRouter router = new ShardingDataSourceRouter();

        // 샤드별 DataSource 맵 구성
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("shard_0", createShard0DataSource());
        targetDataSources.put("shard_1", createShard1DataSource());

        // 라우터에 DataSource들 등록
        router.setTargetDataSources(targetDataSources);
        router.setDefaultTargetDataSource(targetDataSources.get("shard_0"));  // 기본값: shard_0

        log.info("====================================================");
        log.info("[샤딩 설정] DataSource 라우터 초기화 완료");
        log.info("[샤딩 설정] shard_0 → {}", shard0Url);
        log.info("[샤딩 설정] shard_1 → {}", shard1Url);
        log.info("[샤딩 설정] 라우팅 규칙: chatRoomId %% 2 → shard_0(짝수) / shard_1(홀수)");
        log.info("====================================================");

        return router;
    }
}
