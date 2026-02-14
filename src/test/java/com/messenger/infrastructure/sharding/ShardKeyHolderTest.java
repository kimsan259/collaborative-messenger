package com.messenger.infrastructure.sharding;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * ============================================================
 * ShardKeyHolderTest - ThreadLocal 기반 샤드 키 관리 테스트
 * ============================================================
 *
 * 【테스트 대상】
 * ShardKeyHolder의 set(), get(), clear(), resolveShardName() 메서드
 *
 * 【핵심 검증 포인트】
 * 1. 샤드 키 설정 → 올바른 샤드 이름 반환
 * 2. 짝수 ID → shard_0, 홀수 ID → shard_1
 * 3. clear() 호출 후 기본값(shard_0) 반환
 * 4. ThreadLocal이므로 스레드별 독립 동작
 *
 * 【외부 의존성 없음】
 * ThreadLocal만 사용하는 순수 유틸이므로 Mock 불필요!
 * ============================================================
 */
class ShardKeyHolderTest {

    @AfterEach
    void tearDown() {
        // ★ 매 테스트 후 ThreadLocal 정리 (메모리 누수 방지)
        ShardKeyHolder.clear();
    }

    @Test
    @DisplayName("샤드 키 미설정 시 → 기본 샤드(shard_0) 반환")
    void resolveShardName_noKey_returnsDefaultShard() {
        // when: 아무 것도 설정하지 않은 상태
        String shardName = ShardKeyHolder.resolveShardName();

        // then: 기본 샤드 반환
        assertThat(shardName).isEqualTo("shard_0");
    }

    @Test
    @DisplayName("짝수 chatRoomId → shard_0으로 라우팅")
    void resolveShardName_evenId_routesToShard0() {
        // given: 짝수 채팅방 ID
        ShardKeyHolder.set(10L);

        // when
        String shardName = ShardKeyHolder.resolveShardName();

        // then: 10 % 2 = 0 → shard_0
        assertThat(shardName).isEqualTo("shard_0");
    }

    @Test
    @DisplayName("홀수 chatRoomId → shard_1로 라우팅")
    void resolveShardName_oddId_routesToShard1() {
        // given: 홀수 채팅방 ID
        ShardKeyHolder.set(7L);

        // when
        String shardName = ShardKeyHolder.resolveShardName();

        // then: 7 % 2 = 1 → shard_1
        assertThat(shardName).isEqualTo("shard_1");
    }

    @Test
    @DisplayName("clear() 호출 후 → 기본 샤드로 복원")
    void clear_afterSet_returnsDefaultShard() {
        // given: 샤드 키를 설정한 상태
        ShardKeyHolder.set(7L);
        assertThat(ShardKeyHolder.resolveShardName()).isEqualTo("shard_1");

        // when: clear() 호출
        ShardKeyHolder.clear();

        // then: 기본값으로 복원
        assertThat(ShardKeyHolder.resolveShardName()).isEqualTo("shard_0");
    }

    @Test
    @DisplayName("get()으로 현재 설정된 샤드 키를 조회할 수 있음")
    void get_afterSet_returnsSetValue() {
        // given
        ShardKeyHolder.set(42L);

        // when
        Long key = ShardKeyHolder.get();

        // then
        assertThat(key).isEqualTo(42L);
    }

    @Test
    @DisplayName("clear() 후 get() → null 반환")
    void get_afterClear_returnsNull() {
        // given
        ShardKeyHolder.set(42L);
        ShardKeyHolder.clear();

        // when
        Long key = ShardKeyHolder.get();

        // then
        assertThat(key).isNull();
    }

    @Test
    @DisplayName("다양한 ID에 대한 샤드 라우팅 일관성 검증")
    void resolveShardName_variousIds_consistentRouting() {
        // 짝수 ID들 → 모두 shard_0
        for (long id : new long[]{0, 2, 4, 100, 1000}) {
            ShardKeyHolder.set(id);
            assertThat(ShardKeyHolder.resolveShardName())
                    .as("chatRoomId=%d 는 shard_0이어야 함", id)
                    .isEqualTo("shard_0");
            ShardKeyHolder.clear();
        }

        // 홀수 ID들 → 모두 shard_1
        for (long id : new long[]{1, 3, 5, 101, 1001}) {
            ShardKeyHolder.set(id);
            assertThat(ShardKeyHolder.resolveShardName())
                    .as("chatRoomId=%d 는 shard_1이어야 함", id)
                    .isEqualTo("shard_1");
            ShardKeyHolder.clear();
        }
    }

    @Test
    @DisplayName("ThreadLocal은 스레드별로 독립적으로 동작함")
    void threadLocal_independentPerThread() throws InterruptedException {
        // given: 메인 스레드에서 shard_1 설정
        ShardKeyHolder.set(7L);

        // when: 별도 스레드에서 값 확인
        final String[] otherThreadResult = new String[1];
        Thread otherThread = new Thread(() -> {
            // 별도 스레드에서는 아무 것도 설정하지 않았음
            otherThreadResult[0] = ShardKeyHolder.resolveShardName();
        });
        otherThread.start();
        otherThread.join(); // 스레드 완료 대기

        // then: 메인 스레드는 shard_1, 다른 스레드는 기본값 shard_0
        assertThat(ShardKeyHolder.resolveShardName()).isEqualTo("shard_1");
        assertThat(otherThreadResult[0]).isEqualTo("shard_0");
    }
}
