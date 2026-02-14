package com.messenger.report.entity;

import com.messenger.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * ============================================================
 * ReportItem - 업무일지 항목 엔티티
 * ============================================================
 *
 * 【역할】
 * 업무일지(DailyReport) 안의 개별 업무 항목 하나를 나타냅니다.
 * 각 항목은 특정 채팅방에서의 활동을 요약한 것입니다.
 *
 * 【예시】
 * 업무일지 (2026-02-08, 김개발)
 *   ├── 항목1: "프론트엔드 버그 수정 논의" (개발팀 채팅방, 23건, DEVELOPMENT)
 *   ├── 항목2: "주간 스프린트 회의" (전체 회의 채팅방, 15건, MEETING)
 *   └── 항목3: "PR 코드 리뷰" (리뷰 채팅방, 8건, REVIEW)
 *
 * 【테이블】 report_items (Shard 0에 저장)
 * ============================================================
 */
@Entity
@Table(name = "report_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReportItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 업무일지 (N:1 관계) */
    @Setter  // DailyReport.addItem()에서 양방향 관계 설정에 필요
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_report_id")
    private DailyReport dailyReport;

    /** 업무 항목 제목 (예: "프론트엔드 버그 수정 논의") */
    @Column(nullable = false, length = 200)
    private String title;

    /** 상세 내용 (관련 대화의 핵심 내용 요약) */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 어떤 채팅방에서 논의했는지 */
    @Column(name = "chat_room_name", length = 200)
    private String chatRoomName;

    /** 관련 메시지 수 */
    @Column(name = "related_message_count")
    private int relatedMessageCount;

    /** 업무 카테고리: DEVELOPMENT, MEETING, REVIEW, OTHER */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WorkCategory category;
}
