package com.messenger.report.service;

import com.messenger.chat.entity.ChatMessage;
import com.messenger.chat.entity.MessageType;
import com.messenger.report.entity.ReportItem;
import com.messenger.report.entity.WorkCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * ============================================================
 * MessageAnalyzerTest - 메시지 분석기 단위 테스트
 * ============================================================
 *
 * 【테스트 대상】
 * MessageAnalyzer.analyzeMessages() 메서드
 * - 키워드 기반 업무 카테고리 분류가 정확한지 검증
 * - 빈 메시지, 최소 기준 미달, 복수 채팅방 등의 케이스 검증
 *
 * 【외부 의존성 없음】
 * MessageAnalyzer는 순수 로직이므로 Mock 불필요!
 * 입력(메시지 목록)만 있으면 출력(ReportItem 목록)을 검증할 수 있습니다.
 * ============================================================
 */
class MessageAnalyzerTest {

    private MessageAnalyzer messageAnalyzer;

    @BeforeEach
    void setUp() {
        messageAnalyzer = new MessageAnalyzer();
    }

    // ===== 헬퍼 메서드: 테스트용 ChatMessage 생성 =====

    /**
     * 테스트용 ChatMessage를 생성합니다.
     * Builder 대신 직접 생성하여 테스트 가독성을 높입니다.
     */
    private ChatMessage createMessage(Long chatRoomId, Long senderId, String content, LocalDateTime sentAt) {
        return ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(content)
                .messageType(MessageType.TEXT)
                .sentAt(sentAt)
                .build();
    }

    // ===== 테스트 케이스 =====

    @Test
    @DisplayName("빈 메시지 목록 → 빈 결과 반환")
    void analyzeMessages_emptyList_returnsEmpty() {
        // given: 메시지가 없는 경우
        List<ChatMessage> messages = Collections.emptyList();
        Map<Long, String> roomNames = Map.of();

        // when
        List<ReportItem> result = messageAnalyzer.analyzeMessages(messages, roomNames);

        // then: 빈 목록이 반환되어야 함
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("메시지 3건 미만인 채팅방 → 건너뜀 (의미 없는 대화)")
    void analyzeMessages_lessThan3Messages_skipped() {
        // given: 2건의 메시지만 있는 채팅방
        LocalDateTime now = LocalDateTime.now();
        List<ChatMessage> messages = List.of(
                createMessage(1L, 1L, "안녕하세요", now),
                createMessage(1L, 2L, "네 안녕하세요", now.plusMinutes(1))
        );
        Map<Long, String> roomNames = Map.of(1L, "일반 채팅방");

        // when
        List<ReportItem> result = messageAnalyzer.analyzeMessages(messages, roomNames);

        // then: 3건 미만이므로 결과 없음
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("개발 관련 키워드 → DEVELOPMENT 카테고리로 분류")
    void analyzeMessages_developmentKeywords_categorizedAsDevelopment() {
        // given: 개발 관련 키워드가 포함된 메시지
        LocalDateTime now = LocalDateTime.now();
        List<ChatMessage> messages = List.of(
                createMessage(1L, 1L, "코드 리뷰 부탁드립니다", now),
                createMessage(1L, 2L, "버그 수정했습니다", now.plusMinutes(1)),
                createMessage(1L, 1L, "PR 머지 완료, 배포 예정", now.plusMinutes(2)),
                createMessage(1L, 3L, "빌드 테스트 통과했어요", now.plusMinutes(3))
        );
        Map<Long, String> roomNames = Map.of(1L, "백엔드 개발팀");

        // when
        List<ReportItem> result = messageAnalyzer.analyzeMessages(messages, roomNames);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(WorkCategory.DEVELOPMENT);
        assertThat(result.get(0).getChatRoomName()).isEqualTo("백엔드 개발팀");
        assertThat(result.get(0).getRelatedMessageCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("회의 관련 키워드 → MEETING 카테고리로 분류")
    void analyzeMessages_meetingKeywords_categorizedAsMeeting() {
        // given: 회의 관련 키워드가 포함된 메시지
        LocalDateTime now = LocalDateTime.now();
        List<ChatMessage> messages = List.of(
                createMessage(2L, 1L, "오늘 회의 안건 공유합니다", now),
                createMessage(2L, 2L, "미팅 참석 확인해주세요", now.plusMinutes(1)),
                createMessage(2L, 3L, "일정 논의 필요합니다", now.plusMinutes(2))
        );
        Map<Long, String> roomNames = Map.of(2L, "주간 회의방");

        // when
        List<ReportItem> result = messageAnalyzer.analyzeMessages(messages, roomNames);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(WorkCategory.MEETING);
    }

    @Test
    @DisplayName("검토 관련 키워드 → REVIEW 카테고리로 분류")
    void analyzeMessages_reviewKeywords_categorizedAsReview() {
        // given: 리뷰/검토 관련 키워드가 포함된 메시지
        LocalDateTime now = LocalDateTime.now();
        List<ChatMessage> messages = List.of(
                createMessage(3L, 1L, "문서 검토 부탁드립니다", now),
                createMessage(3L, 2L, "피드백 반영했습니다", now.plusMinutes(1)),
                createMessage(3L, 1L, "승인 완료, 코멘트 확인해주세요", now.plusMinutes(2))
        );
        Map<Long, String> roomNames = Map.of(3L, "문서 리뷰방");

        // when
        List<ReportItem> result = messageAnalyzer.analyzeMessages(messages, roomNames);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(WorkCategory.REVIEW);
    }

    @Test
    @DisplayName("키워드가 없는 일반 대화 → OTHER 카테고리로 분류")
    void analyzeMessages_noKeywords_categorizedAsOther() {
        // given: 어떤 카테고리 키워드도 없는 일반 대화
        LocalDateTime now = LocalDateTime.now();
        List<ChatMessage> messages = List.of(
                createMessage(4L, 1L, "점심 뭐 먹을까요?", now),
                createMessage(4L, 2L, "짜장면 어때요?", now.plusMinutes(1)),
                createMessage(4L, 3L, "좋아요 짜장면으로 합시다", now.plusMinutes(2))
        );
        Map<Long, String> roomNames = Map.of(4L, "잡담방");

        // when
        List<ReportItem> result = messageAnalyzer.analyzeMessages(messages, roomNames);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(WorkCategory.OTHER);
    }

    @Test
    @DisplayName("여러 채팅방의 메시지 → 채팅방별로 각각 분석")
    void analyzeMessages_multipleRooms_analyzedSeparately() {
        // given: 2개 채팅방의 메시지
        LocalDateTime now = LocalDateTime.now();
        List<ChatMessage> messages = new ArrayList<>();

        // 채팅방 1: 개발 관련 (5건)
        for (int i = 0; i < 5; i++) {
            messages.add(createMessage(1L, 1L, "코드 수정 및 배포 작업", now.plusMinutes(i)));
        }

        // 채팅방 2: 회의 관련 (3건)
        for (int i = 0; i < 3; i++) {
            messages.add(createMessage(2L, 2L, "회의 안건 논의 참석", now.plusMinutes(i)));
        }

        Map<Long, String> roomNames = Map.of(1L, "개발팀", 2L, "회의실");

        // when
        List<ReportItem> result = messageAnalyzer.analyzeMessages(messages, roomNames);

        // then: 2개 채팅방 → 2개 항목 생성 (메시지 수 많은 순)
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRelatedMessageCount()).isEqualTo(5); // 개발팀이 먼저 (더 많은 메시지)
        assertThat(result.get(1).getRelatedMessageCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("채팅방 이름이 없을 때 → 기본 이름 사용")
    void analyzeMessages_noRoomName_usesDefault() {
        // given: roomNames에 해당 채팅방 이름이 없는 경우
        LocalDateTime now = LocalDateTime.now();
        List<ChatMessage> messages = List.of(
                createMessage(99L, 1L, "메시지1", now),
                createMessage(99L, 2L, "메시지2", now.plusMinutes(1)),
                createMessage(99L, 3L, "메시지3", now.plusMinutes(2))
        );
        Map<Long, String> roomNames = Map.of(); // 빈 맵

        // when
        List<ReportItem> result = messageAnalyzer.analyzeMessages(messages, roomNames);

        // then: 기본 이름 "채팅방 #99" 사용
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChatRoomName()).isEqualTo("채팅방 #99");
    }
}
