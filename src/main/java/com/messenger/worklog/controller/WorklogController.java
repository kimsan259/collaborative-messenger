package com.messenger.worklog.controller;

import com.messenger.chat.event.ChatMessageEvent;
import com.messenger.common.dto.ApiResponse;
import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import com.messenger.infrastructure.kafka.ChatMessageConsumer;
import com.messenger.worklog.dto.WorklogSummaryResponse;
import com.messenger.worklog.service.WorklogService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WorklogController {

    private final WorklogService worklogService;
    private final ChatMessageConsumer chatMessageConsumer;

    @GetMapping("/worklog")
    public String worklogPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("displayName", session.getAttribute("displayName"));
        return "worklog";
    }

    @GetMapping("/api/worklog/today")
    @ResponseBody
    public ResponseEntity<ApiResponse<WorklogSummaryResponse>> todaySummary(
            @RequestParam(required = false) String author,
            HttpSession session) {
        requireLogin(session);
        WorklogSummaryResponse summary = worklogService.generateTodaySummary(author);
        return ResponseEntity.ok(ApiResponse.success("오늘 작업 요약을 생성했습니다.", summary));
    }

    @PostMapping("/api/worklog/today/broadcast/{roomId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> broadcastTodaySummary(
            @PathVariable Long roomId,
            @RequestParam(required = false) String author,
            HttpSession session) {
        Long userId = requireLogin(session);
        String senderName = (String) session.getAttribute("displayName");

        WorklogSummaryResponse summary = worklogService.generateTodaySummary(author);
        String message = worklogService.buildShareText(summary);

        ChatMessageEvent event = ChatMessageEvent.builder()
                .chatRoomId(roomId)
                .senderId(userId)
                .senderName(senderName != null ? senderName : "system")
                .content(message)
                .messageType("SYSTEM")
                .sentAt(LocalDateTime.now())
                .build();

        chatMessageConsumer.consumeEvent(event);

        return ResponseEntity.ok(ApiResponse.success("작업 요약을 메신저로 전송했습니다.",
                Map.of("roomId", roomId, "commitCount", summary.getCommitCount())));
    }

    private Long requireLogin(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}

