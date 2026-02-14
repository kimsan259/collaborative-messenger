package com.messenger.report.entity;

import com.messenger.common.entity.BaseEntity;
import com.messenger.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * DailyReport - 일일 업무일지 엔티티
 * ============================================================
 *
 * 【역할】
 * 매일 오후 6시에 자동 생성되는 업무일지 하나를 나타냅니다.
 * 사용자가 하루 동안 보낸 채팅 메시지를 분석하여 업무 요약을 생성합니다.
 *
 * 【자동 생성 흐름】
 * 1. ReportScheduler가 매일 18:00에 @Scheduled로 실행
 * 2. 모든 활성 사용자에 대해 ReportGenerationService.generateReport() 호출 (@Async)
 * 3. MessageAnalyzer가 채팅 메시지를 분석하여 업무 항목(ReportItem) 생성
 * 4. DailyReport + ReportItem들이 DB에 저장
 * 5. 알림 발송: "업무일지가 생성되었습니다"
 *
 * 【테이블】 daily_reports (Shard 0에 저장)
 * ============================================================
 */
@Entity
@Table(name = "daily_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 업무일지 대상 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** 업무일지 날짜 (예: 2026-02-08) */
    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    /** 자동 생성된 하루 업무 요약 */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** 이 날 보낸 총 메시지 수 */
    @Column(name = "total_message_count")
    private int totalMessageCount;

    /** 이 날 활동한 채팅방 수 */
    @Column(name = "active_room_count")
    private int activeRoomCount;

    /** 업무일지 상태: AUTO_GENERATED, EDITED, CONFIRMED */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReportStatus status;

    /**
     * 업무일지 상세 항목들 (1:N 관계)
     * cascade = ALL: DailyReport 저장 시 ReportItem도 함께 저장
     * orphanRemoval = true: 항목 삭제 시 DB에서도 삭제
     */
    @Builder.Default
    @OneToMany(mappedBy = "dailyReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportItem> items = new ArrayList<>();

    /** 업무 항목을 추가합니다 (양방향 관계 편의 메서드). */
    public void addItem(ReportItem item) {
        this.items.add(item);
        item.setDailyReport(this);
    }

    /** 사용자가 요약을 수정합니다. */
    public void updateSummary(String newSummary) {
        this.summary = newSummary;
        this.status = ReportStatus.EDITED;
    }

    /** 사용자가 확인 완료합니다. */
    public void confirm() {
        this.status = ReportStatus.CONFIRMED;
    }
}
