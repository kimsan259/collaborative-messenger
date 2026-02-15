package com.messenger.chat.controller;

import com.messenger.chat.service.ChatPresenceService;
import com.messenger.common.dto.ApiResponse;
import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PresenceController {

    private final ChatPresenceService chatPresenceService;

    @GetMapping("/api/presence/me")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> myPresence(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        boolean online = chatPresenceService.isOnline(userId);
        return ResponseEntity.ok(ApiResponse.success("내 접속 상태입니다.",
                Map.of("userId", userId, "online", online, "checkedAt", LocalDateTime.now())));
    }

    @GetMapping("/api/presence/{userId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> userPresence(
            @PathVariable Long userId, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("userId");
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        boolean online = chatPresenceService.isOnline(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 접속 상태입니다.",
                Map.of("userId", userId, "online", online, "checkedAt", LocalDateTime.now())));
    }
}
