package com.messenger.report.controller;

import com.messenger.common.dto.ApiResponse;
import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import com.messenger.report.dto.ReportResponse;
import com.messenger.report.dto.ReportUpdateRequest;
import com.messenger.report.entity.DailyReport;
import com.messenger.report.repository.DailyReportRepository;
import com.messenger.report.service.ReportGenerationService;
import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * ReportController - 업무일지 컨트롤러
 * ============================================================
 *
 * 【엔드포인트 목록】
 * GET  /reports              → 업무일지 목록 페이지 (Thymeleaf)
 * GET  /reports/{id}         → 업무일지 상세 페이지 (Thymeleaf)
 * GET  /api/reports          → 업무일지 목록 API (JSON)
 * GET  /api/reports/{id}     → 업무일지 상세 API (JSON)
 * PUT  /api/reports/{id}     → 업무일지 수정 API (JSON)
 * POST /api/reports/{id}/confirm → 업무일지 확인 API (JSON)
 * POST /api/reports/generate → 업무일지 수동 생성 API (디버깅용)
 * ============================================================
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ReportController {

    private final DailyReportRepository dailyReportRepository;
    private final ReportGenerationService reportGenerationService;
    private final UserRepository userRepository;

    // ===== 페이지 요청 =====

    /** 업무일지 목록 페이지 */
    @GetMapping("/reports")
    public String reportListPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            List<DailyReport> reports = dailyReportRepository.findByUserIdOrderByReportDateDesc(userId);
            List<ReportResponse> responses = reports.stream()
                    .map(ReportResponse::from)
                    .collect(Collectors.toList());
            model.addAttribute("reports", responses);
        }
        return "report-list";
    }

    /** 업무일지 상세 페이지 */
    @GetMapping("/reports/{id}")
    public String reportDetailPage(@PathVariable Long id, Model model) {
        DailyReport report = dailyReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        model.addAttribute("report", ReportResponse.from(report));
        return "report-detail";
    }

    // ===== REST API =====

    /** 업무일지 목록 조회 API */
    @GetMapping("/api/reports")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReports(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        List<DailyReport> reports = dailyReportRepository.findByUserIdOrderByReportDateDesc(userId);
        List<ReportResponse> responses = reports.stream()
                .map(ReportResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("업무일지 목록을 조회했습니다.", responses));
    }

    /** 업무일지 상세 조회 API */
    @GetMapping("/api/reports/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(@PathVariable Long id) {
        DailyReport report = dailyReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.success("업무일지를 조회했습니다.", ReportResponse.from(report)));
    }

    /** 업무일지 요약 수정 API */
    @PutMapping("/api/reports/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<ReportResponse>> updateReport(
            @PathVariable Long id,
            @RequestBody ReportUpdateRequest request) {

        DailyReport report = dailyReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        report.updateSummary(request.getSummary());
        dailyReportRepository.save(report);

        log.info("[업무일지 수정] 리포트ID={}", id);
        return ResponseEntity.ok(ApiResponse.success("업무일지가 수정되었습니다.", ReportResponse.from(report)));
    }

    /** 업무일지 확인 완료 API */
    @PostMapping("/api/reports/{id}/confirm")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> confirmReport(@PathVariable Long id) {
        DailyReport report = dailyReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        report.confirm();
        dailyReportRepository.save(report);

        log.info("[업무일지 확인] 리포트ID={}", id);
        return ResponseEntity.ok(ApiResponse.success("업무일지가 확인되었습니다."));
    }

    /**
     * 【업무일지 수동 생성 API (디버깅/테스트용)】
     * 스케줄러를 기다리지 않고 즉시 업무일지를 생성할 수 있습니다.
     */
    @PostMapping("/api/reports/generate")
    @ResponseBody
    public ResponseEntity<ApiResponse<ReportResponse>> generateReport(
            HttpSession session,
            @RequestParam(required = false) String date) {

        Long userId = (Long) session.getAttribute("userId");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate targetDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();

        DailyReport report = reportGenerationService.generateReport(user, targetDate);
        if (report == null) {
            return ResponseEntity.ok(ApiResponse.success("해당 날짜의 업무일지가 이미 존재합니다."));
        }

        return ResponseEntity.ok(ApiResponse.success("업무일지가 생성되었습니다.", ReportResponse.from(report)));
    }
}
