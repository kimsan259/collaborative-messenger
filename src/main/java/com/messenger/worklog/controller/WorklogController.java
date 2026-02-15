package com.messenger.worklog.controller;

import com.messenger.chat.dto.ChatRoomResponse;
import com.messenger.chat.event.ChatMessageEvent;
import com.messenger.chat.service.ChatRoomService;
import com.messenger.common.dto.ApiResponse;
import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import com.messenger.infrastructure.kafka.ChatMessageConsumer;
import com.messenger.user.dto.FriendshipResponse;
import com.messenger.user.entity.User;
import com.messenger.user.service.FriendshipService;
import com.messenger.user.service.UserService;
import com.messenger.worklog.dto.WorklogAuthorOption;
import com.messenger.worklog.dto.WorklogSummaryResponse;
import com.messenger.worklog.service.WorklogService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class WorklogController {

    private final WorklogService worklogService;
    private final ChatMessageConsumer chatMessageConsumer;
    private final FriendshipService friendshipService;
    private final ChatRoomService chatRoomService;
    private final UserService userService;

    @GetMapping("/worklog")
    public String worklogPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("displayName", session.getAttribute("displayName"));
        return "worklog";
    }

    @GetMapping("/api/worklog/authors")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<WorklogAuthorOption>>> authors(HttpSession session) {
        Long userId = requireLogin(session);
        User me = userService.findEntityById(userId);
        List<FriendshipResponse> friends = friendshipService.getFriends(userId);

        Set<Long> seen = new LinkedHashSet<>();
        List<WorklogAuthorOption> options = new java.util.ArrayList<>();

        options.add(WorklogAuthorOption.builder()
                .userId(me.getId())
                .displayName(me.getDisplayName())
                .username(me.getUsername())
                .githubAuthor(userService.resolveGithubAuthor(me))
                .self(true)
                .build());
        seen.add(me.getId());

        for (FriendshipResponse friend : friends) {
            if (!seen.add(friend.getFriendId())) {
                continue;
            }
            User friendUser = userService.findEntityById(friend.getFriendId());
            options.add(WorklogAuthorOption.builder()
                    .userId(friendUser.getId())
                    .displayName(friendUser.getDisplayName())
                    .username(friendUser.getUsername())
                    .githubAuthor(userService.resolveGithubAuthor(friendUser))
                    .self(false)
                    .build());
        }

        return ResponseEntity.ok(ApiResponse.success("요약 작성자 목록을 조회했습니다.", options));
    }

    @PutMapping("/api/worklog/github-author")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, String>>> updateGithubAuthor(
            @RequestBody Map<String, String> request,
            HttpSession session) {
        Long userId = requireLogin(session);
        String githubAuthor = request.get("githubAuthor");
        var updated = userService.updateGithubUsername(userId, githubAuthor);
        String effective = updated.getGithubUsername() == null || updated.getGithubUsername().isBlank()
                ? updated.getUsername()
                : updated.getGithubUsername();
        return ResponseEntity.ok(ApiResponse.success("GitHub 작성자 매핑을 저장했습니다.",
                Map.of("githubAuthor", effective)));
    }

    @GetMapping("/api/worklog/friends")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> friends(HttpSession session) {
        Long userId = requireLogin(session);
        return ResponseEntity.ok(ApiResponse.success("친구 목록을 조회했습니다.", friendshipService.getFriends(userId)));
    }

    @GetMapping("/api/worklog/today")
    @ResponseBody
    public ResponseEntity<ApiResponse<WorklogSummaryResponse>> todaySummary(
            @RequestParam(required = false) String author,
            HttpSession session) {
        requireLogin(session);
        String resolvedAuthor = resolveAuthor(session, author);
        WorklogSummaryResponse summary = worklogService.generateTodaySummary(resolvedAuthor);
        return ResponseEntity.ok(ApiResponse.success("오늘 작업 요약을 생성했습니다.", summary));
    }

    @PostMapping("/api/worklog/today/broadcast/friend/{friendId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, Object>>> broadcastTodaySummaryToFriend(
            @PathVariable Long friendId,
            @RequestParam(required = false) String author,
            HttpSession session) {

        Long userId = requireLogin(session);
        boolean isFriend = friendshipService.getFriends(userId).stream()
                .anyMatch(f -> friendId.equals(f.getFriendId()));
        if (!isFriend) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String senderName = (String) session.getAttribute("displayName");
        String resolvedAuthor = resolveAuthor(session, author);
        WorklogSummaryResponse summary = worklogService.generateTodaySummary(resolvedAuthor);
        String message = worklogService.buildShareText(summary);

        ChatRoomResponse dmRoom = chatRoomService.getOrCreateDirectRoom(userId, friendId);

        ChatMessageEvent event = ChatMessageEvent.builder()
                .chatRoomId(dmRoom.getId())
                .senderId(userId)
                .senderName(senderName != null ? senderName : "system")
                .content(message)
                .messageType("SYSTEM")
                .sentAt(LocalDateTime.now())
                .build();

        chatMessageConsumer.consumeEvent(event);

        return ResponseEntity.ok(ApiResponse.success("작업 요약을 친구에게 전송했습니다.",
                Map.of("friendId", friendId, "roomId", dmRoom.getId(), "commitCount", summary.getCommitCount())));
    }

    private Long requireLogin(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private String resolveAuthor(HttpSession session, String requestedAuthor) {
        if (requestedAuthor != null && !requestedAuthor.isBlank()) {
            return requestedAuthor.trim();
        }
        Long userId = requireLogin(session);
        User me = userService.findEntityById(userId);
        return userService.resolveGithubAuthor(me);
    }
}
