package com.messenger.report.service;

import com.messenger.chat.entity.ChatMessage;
import com.messenger.report.entity.ReportItem;
import com.messenger.report.entity.WorkCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================
 * MessageAnalyzer - 채팅 메시지 분석기 (업무일지의 핵심 로직)
 * ============================================================
 *
 * 【역할】
 * 하루 동안의 채팅 메시지를 분석하여 업무일지 항목(ReportItem)으로 변환합니다.
 * AI/LLM 없이 키워드 빈도 분석으로 업무를 분류합니다.
 *
 * 【분석 절차 (5단계)】
 * 1단계: 채팅방별로 메시지 그룹화
 * 2단계: 각 채팅방의 메시지에서 키워드 빈도 분석
 * 3단계: 가장 빈도 높은 키워드 기반으로 업무 카테고리 결정
 * 4단계: 채팅방 이름 + 키워드 조합으로 업무 제목 자동 생성
 * 5단계: ReportItem 목록 반환
 *
 * 【키워드 기반 분류 규칙】
 * - DEVELOPMENT: "코드", "버그", "수정", "배포", "개발", "구현", "PR", "커밋", "빌드"
 * - MEETING:     "회의", "미팅", "논의", "결정", "안건", "일정", "참석"
 * - REVIEW:      "검토", "확인", "피드백", "리뷰", "승인", "반려"
 * - OTHER:       위 키워드에 해당하지 않는 경우
 *
 * 【향후 개선 방향】
 * - AI/LLM API 연동하여 더 정교한 분석 가능
 * - 사용자가 키워드를 커스터마이징하는 기능 추가 가능
 * ============================================================
 */
@Slf4j
@Component
public class MessageAnalyzer {

    /**
     * 업무 카테고리별 키워드 맵.
     * 메시지에서 이 키워드들의 빈도를 분석하여 카테고리를 결정합니다.
     */
    private static final Map<WorkCategory, List<String>> CATEGORY_KEYWORDS = Map.of(
            WorkCategory.DEVELOPMENT, List.of(
                    "코드", "버그", "수정", "배포", "개발", "구현", "PR", "커밋",
                    "빌드", "테스트", "에러", "오류", "디버그", "API", "서버", "DB",
                    "프론트", "백엔드", "기능", "모듈", "브랜치", "머지"),
            WorkCategory.MEETING, List.of(
                    "회의", "미팅", "논의", "결정", "안건", "일정", "참석",
                    "발표", "공유", "보고", "계획", "전략", "목표"),
            WorkCategory.REVIEW, List.of(
                    "검토", "확인", "피드백", "리뷰", "승인", "반려",
                    "수정요청", "코멘트", "코드리뷰")
    );

    /**
     * 【메시지 분석 메인 메서드】
     *
     * 사용자의 하루 메시지를 분석하여 업무일지 항목 목록을 생성합니다.
     *
     * @param messages  분석할 메시지 목록 (하루치)
     * @param roomNames 채팅방 ID → 채팅방 이름 매핑
     * @return 업무일지 항목(ReportItem) 목록
     */
    public List<ReportItem> analyzeMessages(List<ChatMessage> messages, Map<Long, String> roomNames) {
        log.info("[메시지 분석] 분석 대상 메시지 수={}", messages.size());

        if (messages.isEmpty()) {
            log.info("[메시지 분석] 분석할 메시지가 없습니다.");
            return Collections.emptyList();
        }

        // ===== 1단계: 채팅방별로 메시지 그룹화 =====
        // 같은 채팅방의 메시지를 하나의 그룹으로 묶습니다
        Map<Long, List<ChatMessage>> messagesByRoom = messages.stream()
                .collect(Collectors.groupingBy(ChatMessage::getChatRoomId));

        log.debug("[메시지 분석] 활성 채팅방 수={}", messagesByRoom.size());

        // ===== 2~5단계: 각 채팅방에 대해 업무 항목 생성 =====
        List<ReportItem> items = new ArrayList<>();

        for (Map.Entry<Long, List<ChatMessage>> entry : messagesByRoom.entrySet()) {
            Long roomId = entry.getKey();
            List<ChatMessage> roomMessages = entry.getValue();

            // 메시지가 3건 미만인 채팅방은 건너뜀 (너무 적은 대화는 의미 없음)
            if (roomMessages.size() < 3) {
                log.debug("[메시지 분석] 채팅방ID={} 메시지 {}건 - 최소 기준 미달로 건너뜀", roomId, roomMessages.size());
                continue;
            }

            // 2단계: 전체 메시지를 하나의 텍스트로 합침
            String allText = roomMessages.stream()
                    .map(ChatMessage::getContent)
                    .collect(Collectors.joining(" "));

            // 3단계: 키워드 분석으로 업무 카테고리 결정
            WorkCategory category = determineCategory(allText);

            // 4단계: 업무 제목 자동 생성
            String roomName = roomNames.getOrDefault(roomId, "채팅방 #" + roomId);
            String title = generateTitle(roomName, category, roomMessages.size());

            // 5단계: ReportItem 생성
            ReportItem item = ReportItem.builder()
                    .title(title)
                    .description(generateDescription(roomMessages, category))
                    .chatRoomName(roomName)
                    .relatedMessageCount(roomMessages.size())
                    .category(category)
                    .build();

            items.add(item);
            log.debug("[메시지 분석] 항목 생성: 채팅방={}, 카테고리={}, 메시지수={}", roomName, category, roomMessages.size());
        }

        // 메시지 수가 많은 순으로 정렬 (가장 활발한 업무가 위에)
        items.sort((a, b) -> Integer.compare(b.getRelatedMessageCount(), a.getRelatedMessageCount()));

        log.info("[메시지 분석 완료] 생성된 업무 항목 수={}", items.size());
        return items;
    }

    /**
     * 【업무 카테고리 결정】
     * 메시지 텍스트에서 각 카테고리의 키워드 출현 횟수를 세어,
     * 가장 많이 나온 카테고리로 분류합니다.
     */
    private WorkCategory determineCategory(String text) {
        WorkCategory bestCategory = WorkCategory.OTHER;
        int maxCount = 0;

        for (Map.Entry<WorkCategory, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int count = 0;
            for (String keyword : entry.getValue()) {
                // 텍스트에서 키워드가 몇 번 나오는지 카운트
                int idx = 0;
                while ((idx = text.indexOf(keyword, idx)) != -1) {
                    count++;
                    idx += keyword.length();
                }
            }

            if (count > maxCount) {
                maxCount = count;
                bestCategory = entry.getKey();
            }
        }

        return bestCategory;
    }

    /**
     * 【업무 제목 자동 생성】
     * 채팅방 이름과 카테고리를 조합하여 자연스러운 제목을 만듭니다.
     */
    private String generateTitle(String roomName, WorkCategory category, int messageCount) {
        String categoryLabel = switch (category) {
            case DEVELOPMENT -> "개발 관련 논의";
            case MEETING -> "회의/미팅";
            case REVIEW -> "검토/리뷰";
            case OTHER -> "업무 대화";
        };
        return String.format("[%s] %s (%d건)", roomName, categoryLabel, messageCount);
    }

    /**
     * 【업무 설명 자동 생성】
     * 해당 채팅방 대화의 핵심 내용을 간략히 요약합니다.
     * (현재는 시간대별 활동량 + 주요 키워드를 나열하는 방식)
     */
    private String generateDescription(List<ChatMessage> messages, WorkCategory category) {
        // 시간대별 메시지 수 분석
        Map<Integer, Long> hourlyCount = messages.stream()
                .filter(m -> m.getSentAt() != null)
                .collect(Collectors.groupingBy(
                        m -> m.getSentAt().getHour(),
                        Collectors.counting()));

        // 가장 활발한 시간대 찾기
        Optional<Map.Entry<Integer, Long>> peakHour = hourlyCount.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        StringBuilder desc = new StringBuilder();
        desc.append(String.format("총 %d건의 메시지가 교환되었습니다.\n", messages.size()));

        if (peakHour.isPresent()) {
            desc.append(String.format("가장 활발한 시간대: %d시 (%d건)\n",
                    peakHour.get().getKey(), peakHour.get().getValue()));
        }

        desc.append(String.format("업무 분류: %s", getCategoryDescription(category)));

        return desc.toString();
    }

    /** 카테고리별 한글 설명 */
    private String getCategoryDescription(WorkCategory category) {
        return switch (category) {
            case DEVELOPMENT -> "소프트웨어 개발 관련 논의";
            case MEETING -> "회의 및 미팅 진행";
            case REVIEW -> "코드/문서 검토 및 피드백";
            case OTHER -> "일반 업무 대화";
        };
    }
}
