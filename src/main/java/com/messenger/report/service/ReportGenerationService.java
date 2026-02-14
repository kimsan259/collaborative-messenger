package com.messenger.report.service;

import com.messenger.chat.entity.ChatMessage;
import com.messenger.chat.repository.ChatMessageRepository;
import com.messenger.chat.repository.ChatRoomMemberRepository;
import com.messenger.chat.repository.ChatRoomRepository;
import com.messenger.report.entity.DailyReport;
import com.messenger.report.entity.ReportItem;
import com.messenger.report.entity.ReportStatus;
import com.messenger.report.repository.DailyReportRepository;
import com.messenger.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================
 * ReportGenerationService - 업무일지 생성 서비스
 * ============================================================
 *
 * 【역할】
 * 특정 사용자의 하루 채팅 메시지를 분석하여 업무일지를 자동 생성합니다.
 *
 * 【호출 시점】
 * - ReportScheduler가 매일 18:00에 이 서비스를 호출합니다.
 * - @Async("reportGenerationExecutor"): 전용 스레드풀에서 비동기 실행
 *   → 사용자 50명의 업무일지를 동시에 생성해도 메인 스레드에 영향 없음
 *
 * 【처리 흐름】
 * 1. 해당 사용자의 당일 메시지를 모든 샤드에서 조회
 * 2. MessageAnalyzer로 메시지 분석 → ReportItem 목록 생성
 * 3. DailyReport 엔티티 생성 + ReportItem 연결
 * 4. DB에 저장
 *
 * 【샤딩 환경에서의 특이사항】
 * 업무일지는 "사용자 기준"이므로, 해당 사용자가 참여한 모든 채팅방의
 * 메시지를 조회해야 합니다. 채팅방은 여러 샤드에 분산되어 있으므로,
 * 모든 샤드에서 각각 쿼리를 실행한 후 결과를 합쳐야 합니다.
 * → Phase 6(샤딩 구현) 이후에 샤드별 조회 로직이 추가될 예정
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final DailyReportRepository dailyReportRepository;
    private final MessageAnalyzer messageAnalyzer;

    /**
     * 【업무일지 자동 생성 (비동기)】
     *
     * @Async("reportGenerationExecutor"):
     *   - AsyncConfig에서 정의한 "report-gen-" 스레드풀에서 실행
     *   - 호출자(ReportScheduler)는 이 메서드가 끝나기를 기다리지 않음
     *   - 로그에서 "report-gen-1" 같은 스레드명으로 식별 가능
     *
     * @param user 업무일지를 생성할 사용자
     * @param date 업무일지 대상 날짜
     */
    @Async("reportGenerationExecutor")
    @Transactional
    public void generateReportAsync(User user, LocalDate date) {
        try {
            generateReport(user, date);
        } catch (Exception e) {
            log.error("[업무일지 생성 실패] 사용자={}, 날짜={}, 에러={}",
                    user.getUsername(), date, e.getMessage(), e);
        }
    }

    /**
     * 【업무일지 생성 핵심 로직 (동기)】
     *
     * 수동 생성 시에도 사용할 수 있도록 별도 메서드로 분리했습니다.
     *
     * @param user 업무일지를 생성할 사용자
     * @param date 업무일지 대상 날짜
     * @return 생성된 DailyReport
     */
    @Transactional
    public DailyReport generateReport(User user, LocalDate date) {
        log.info("[업무일지 생성 시작] 사용자={}, 날짜={}", user.getUsername(), date);

        // 이미 해당 날짜의 업무일지가 있는지 확인
        if (dailyReportRepository.existsByUserIdAndReportDate(user.getId(), date)) {
            log.info("[업무일지 생성 스킵] 이미 존재함 - 사용자={}, 날짜={}", user.getUsername(), date);
            return dailyReportRepository.findByUserIdAndReportDate(user.getId(), date).orElse(null);
        }

        // ===== 1단계: 해당 날짜의 메시지 조회 =====
        LocalDateTime dayStart = date.atStartOfDay();           // 00:00:00
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);      // 23:59:59

        // ★ 현재는 기본 DataSource(shard_0)에서만 조회
        // Phase 6 이후: 모든 샤드에서 조회하여 합치는 로직 추가
        List<ChatMessage> messages = chatMessageRepository
                .findBySenderIdAndSentAtBetween(user.getId(), dayStart, dayEnd);

        log.info("[업무일지] 사용자={}, 메시지 수={}", user.getUsername(), messages.size());

        // ===== 2단계: 채팅방 이름 맵 생성 =====
        // 메시지에서 채팅방 ID를 추출하고, 채팅방 이름을 조회
        Map<Long, String> roomNames = messages.stream()
                .map(ChatMessage::getChatRoomId)
                .distinct()
                .collect(Collectors.toMap(
                        roomId -> roomId,
                        roomId -> chatRoomRepository.findById(roomId)
                                .map(room -> room.getName())
                                .orElse("알 수 없는 채팅방")
                ));

        // ===== 3단계: MessageAnalyzer로 메시지 분석 =====
        List<ReportItem> items = messageAnalyzer.analyzeMessages(messages, roomNames);

        // ===== 4단계: 업무 요약 생성 =====
        String summary = generateSummary(user.getDisplayName(), date, messages.size(), roomNames.size(), items);

        // ===== 5단계: DailyReport 엔티티 생성 =====
        DailyReport report = DailyReport.builder()
                .user(user)
                .reportDate(date)
                .summary(summary)
                .totalMessageCount(messages.size())
                .activeRoomCount(roomNames.size())
                .status(ReportStatus.AUTO_GENERATED)
                .build();

        // ReportItem들을 DailyReport에 연결
        for (ReportItem item : items) {
            report.addItem(item);
        }

        // ===== 6단계: DB에 저장 =====
        DailyReport savedReport = dailyReportRepository.save(report);

        log.info("[업무일지 생성 완료] 사용자={}, 날짜={}, 리포트ID={}, 항목수={}",
                user.getUsername(), date, savedReport.getId(), items.size());

        return savedReport;
    }

    /**
     * 【업무 요약 텍스트 생성】
     * 하루 업무를 한 단락으로 요약합니다.
     */
    private String generateSummary(String displayName, LocalDate date,
                                    int messageCount, int roomCount, List<ReportItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s님의 %s 업무일지\n\n", displayName, date));
        sb.append(String.format("오늘 %d개의 채팅방에서 총 %d건의 메시지를 교환했습니다.\n\n", roomCount, messageCount));

        if (!items.isEmpty()) {
            sb.append("주요 업무:\n");
            for (int i = 0; i < items.size(); i++) {
                ReportItem item = items.get(i);
                sb.append(String.format("%d. %s\n", i + 1, item.getTitle()));
            }
        } else {
            sb.append("분석할 수 있는 주요 업무가 없습니다.");
        }

        return sb.toString();
    }
}
