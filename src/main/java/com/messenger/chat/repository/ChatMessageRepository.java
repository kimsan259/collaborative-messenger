package com.messenger.chat.repository;

import com.messenger.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================
 * ChatMessageRepository - 채팅 메시지 데이터 접근 인터페이스
 * ============================================================
 *
 * 【주의: 샤딩 환경에서의 동작】
 * 이 Repository는 AbstractRoutingDataSource에 의해
 * ShardKeyHolder에 설정된 샤드로 자동 라우팅됩니다.
 *
 * 예: ShardKeyHolder.set(7L) → chatRoomId=7 → 7%2=1 → Shard 1에서 쿼리 실행
 *
 * 쿼리를 실행하기 전에 반드시 ShardKeyHolder에 샤드 키를 설정해야 합니다.
 * (ShardingAspect가 @ShardBy 어노테이션으로 자동 처리합니다)
 * ============================================================
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 채팅방의 메시지를 시간순으로 조회합니다.
     * ★ 이 쿼리는 샤드 키(chatRoomId)가 조건에 포함되어 있으므로
     *   하나의 샤드에서만 실행됩니다 (효율적).
     */
    List<ChatMessage> findByChatRoomIdOrderBySentAtDesc(Long chatRoomId, Pageable pageable);

    /**
     * 특정 사용자가 특정 날짜 범위에 보낸 메시지를 조회합니다.
     * ★ 업무일지 생성 시 사용: 이 쿼리는 모든 샤드에서 실행해야 합니다.
     *   (사용자는 여러 채팅방에 메시지를 보내므로 여러 샤드에 데이터가 분산)
     *   ReportGenerationService에서 샤드별로 각각 호출합니다.
     */
    List<ChatMessage> findBySenderIdAndSentAtBetween(
            Long senderId, LocalDateTime start, LocalDateTime end);

    /**
     * 특정 채팅방에서 특정 시간 이후의 메시지 수를 조회합니다.
     * "읽지 않은 메시지 수" 계산에 사용됩니다.
     */
    long countByChatRoomIdAndSentAtAfter(Long chatRoomId, LocalDateTime after);

    /** 특정 채팅방의 전체 메시지 수 */
    long countByChatRoomId(Long chatRoomId);

    /** 특정 채팅방의 가장 최근 메시지 1건 조회 (미리보기용) */
    List<ChatMessage> findTop1ByChatRoomIdOrderBySentAtDesc(Long chatRoomId);
}
