package com.messenger.infrastructure.sharding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * ============================================================
 * ShardingDataSourceRouter - 동적 DataSource 라우터
 * ============================================================
 *
 * 【역할】
 * 쿼리가 실행될 때마다 어떤 MySQL 인스턴스(DataSource)를 사용할지 동적으로 결정합니다.
 *
 * 【동작 원리 - AbstractRoutingDataSource】
 *
 * Spring의 AbstractRoutingDataSource는 여러 개의 DataSource를 가지고 있다가,
 * 쿼리가 실행될 때 determineCurrentLookupKey() 메서드를 호출하여
 * 어떤 DataSource를 사용할지 결정합니다.
 *
 *   ┌─────────────────────────────────┐
 *   │   ShardingDataSourceRouter      │
 *   │                                 │
 *   │   "shard_0" → MySQL 3307 ─┐    │
 *   │   "shard_1" → MySQL 3308 ─┘    │
 *   │                                 │
 *   │   determineCurrentLookupKey()   │
 *   │   → ShardKeyHolder.resolve()   │
 *   │   → "shard_0" 또는 "shard_1"   │
 *   └─────────────────────────────────┘
 *
 * 【전체 라우팅 흐름】
 * 1. ShardKeyHolder.set(chatRoomId=7)          ← 샤드 키 설정
 * 2. chatMessageRepository.save(message)        ← JPA가 쿼리 실행
 * 3. → ShardingDataSourceRouter에게 DataSource 요청
 * 4. → determineCurrentLookupKey() 호출
 * 5. → ShardKeyHolder.resolveShardName() → "shard_1" (7%2=1)
 * 6. → "shard_1"에 해당하는 MySQL 3308로 쿼리 실행
 * 7. ShardKeyHolder.clear()                     ← 샤드 키 해제
 * ============================================================
 */
@Slf4j
public class ShardingDataSourceRouter extends AbstractRoutingDataSource {

    /**
     * 【현재 쿼리가 사용할 DataSource의 키를 반환】
     *
     * Spring이 DB 쿼리를 실행하기 직전에 이 메서드를 자동으로 호출합니다.
     * 반환값이 ShardingConfig에서 등록한 DataSource 맵의 키("shard_0" 또는 "shard_1")와
     * 매칭되어 해당 DataSource가 사용됩니다.
     *
     * @return "shard_0" 또는 "shard_1"
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String shardName = ShardKeyHolder.resolveShardName();
        log.debug("[샤딩 라우터] 현재 요청이 라우팅되는 샤드: {}", shardName);
        return shardName;
    }
}
