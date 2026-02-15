package com.messenger.report.controller;

import com.messenger.common.dto.ApiResponse;
import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ReportController {

    @GetMapping("/reports")
    public String reportListPage() {
        return "redirect:/worklog";
    }

    @GetMapping("/reports/{id}")
    public String reportDetailPage(@PathVariable Long id) {
        return "redirect:/worklog";
    }

    @GetMapping("/api/reports")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> getReports() {
        throw new BusinessException(ErrorCode.REPORTS_DISABLED);
    }

    @GetMapping("/api/reports/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> getReport(@PathVariable Long id) {
        throw new BusinessException(ErrorCode.REPORTS_DISABLED);
    }

    @PutMapping("/api/reports/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> updateReport(
            @PathVariable Long id,
            @RequestBody(required = false) Object request) {
        throw new BusinessException(ErrorCode.REPORTS_DISABLED);
    }

    @PostMapping("/api/reports/{id}/confirm")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> confirmReport(@PathVariable Long id) {
        throw new BusinessException(ErrorCode.REPORTS_DISABLED);
    }

    @PostMapping("/api/reports/generate")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> generateReport() {
        throw new BusinessException(ErrorCode.REPORTS_DISABLED);
    }
}
