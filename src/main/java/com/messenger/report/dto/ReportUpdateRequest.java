package com.messenger.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ============================================================
 * ReportUpdateRequest - 업무일지 수정 요청 DTO
 * ============================================================
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportUpdateRequest {

    /** 수정할 업무 요약 */
    private String summary;
}
