package com.messenger.report.dto;

import com.messenger.report.entity.DailyReport;
import com.messenger.report.entity.ReportItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * ReportResponse - 업무일지 응답 DTO
 * ============================================================
 */
@Getter
@Builder
@AllArgsConstructor
public class ReportResponse {

    private Long id;
    private Long userId;
    private String userName;
    private LocalDate reportDate;
    private String summary;
    private int totalMessageCount;
    private int activeRoomCount;
    private String status;
    private List<ReportItemResponse> items;

    /** DailyReport 엔티티를 응답 DTO로 변환합니다. */
    public static ReportResponse from(DailyReport report) {
        return ReportResponse.builder()
                .id(report.getId())
                .userId(report.getUser().getId())
                .userName(report.getUser().getDisplayName())
                .reportDate(report.getReportDate())
                .summary(report.getSummary())
                .totalMessageCount(report.getTotalMessageCount())
                .activeRoomCount(report.getActiveRoomCount())
                .status(report.getStatus().name())
                .items(report.getItems().stream()
                        .map(ReportItemResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 업무일지 항목 응답 DTO (내부 클래스)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ReportItemResponse {
        private Long id;
        private String title;
        private String description;
        private String chatRoomName;
        private int relatedMessageCount;
        private String category;

        public static ReportItemResponse from(ReportItem item) {
            return ReportItemResponse.builder()
                    .id(item.getId())
                    .title(item.getTitle())
                    .description(item.getDescription())
                    .chatRoomName(item.getChatRoomName())
                    .relatedMessageCount(item.getRelatedMessageCount())
                    .category(item.getCategory().name())
                    .build();
        }
    }
}
