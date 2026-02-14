package com.messenger.user.controller;

import com.messenger.common.dto.ApiResponse;
import com.messenger.user.dto.FriendshipResponse;
import com.messenger.user.service.FriendshipService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 * FriendshipController - 친구 관리 컨트롤러
 * ============================================================
 *
 * 【엔드포인트 목록】
 * GET  /friends                        → 친구 목록 페이지 (Thymeleaf)
 * GET  /api/friends                    → 내 친구 목록 API
 * GET  /api/friends/requests/received  → 받은 친구 요청 목록 API
 * GET  /api/friends/requests/sent      → 보낸 친구 요청 목록 API
 * POST /api/friends/request/{userId}   → 친구 요청 보내기 API
 * POST /api/friends/accept/{id}        → 친구 요청 수락 API
 * POST /api/friends/reject/{id}        → 친구 요청 거절 API
 * DELETE /api/friends/{id}             → 친구 삭제 API
 * GET  /api/friends/search             → 사용자 검색 API
 * ============================================================
 */
@Controller
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    // ===== 페이지 요청 =====

    /** 친구 목록 페이지 */
    @GetMapping("/friends")
    public String friendsPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        model.addAttribute("userId", userId);
        model.addAttribute("displayName", session.getAttribute("displayName"));
        return "friends";
    }

    // ===== REST API =====

    /** 내 친구 목록 조회 */
    @GetMapping("/api/friends")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getFriends(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        List<FriendshipResponse> friends = friendshipService.getFriends(userId);
        return ResponseEntity.ok(ApiResponse.success("친구 목록을 조회했습니다.", friends));
    }

    /** 받은 친구 요청 목록 */
    @GetMapping("/api/friends/requests/received")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getReceivedRequests(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        List<FriendshipResponse> requests = friendshipService.getPendingReceivedRequests(userId);
        return ResponseEntity.ok(ApiResponse.success("받은 친구 요청을 조회했습니다.", requests));
    }

    /** 보낸 친구 요청 목록 */
    @GetMapping("/api/friends/requests/sent")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getSentRequests(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        List<FriendshipResponse> requests = friendshipService.getPendingSentRequests(userId);
        return ResponseEntity.ok(ApiResponse.success("보낸 친구 요청을 조회했습니다.", requests));
    }

    /** 친구 요청 보내기 */
    @PostMapping("/api/friends/request/{receiverId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<FriendshipResponse>> sendRequest(
            @PathVariable Long receiverId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        FriendshipResponse response = friendshipService.sendFriendRequest(userId, receiverId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청을 보냈습니다.", response));
    }

    /** 친구 요청 수락 */
    @PostMapping("/api/friends/accept/{friendshipId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<FriendshipResponse>> acceptRequest(
            @PathVariable Long friendshipId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        FriendshipResponse response = friendshipService.acceptFriendRequest(friendshipId, userId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청을 수락했습니다.", response));
    }

    /** 친구 요청 거절 */
    @PostMapping("/api/friends/reject/{friendshipId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> rejectRequest(
            @PathVariable Long friendshipId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        friendshipService.rejectFriendRequest(friendshipId, userId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청을 거절했습니다."));
    }

    /** 친구 삭제 */
    @DeleteMapping("/api/friends/{friendshipId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> removeFriend(
            @PathVariable Long friendshipId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        friendshipService.removeFriend(friendshipId, userId);
        return ResponseEntity.ok(ApiResponse.success("친구를 삭제했습니다."));
    }

    /** 사용자 검색 (친구 추가용) */
    @GetMapping("/api/friends/search")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> searchUsers(
            @RequestParam String keyword, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        List<FriendshipResponse> results = friendshipService.searchUsers(keyword, userId);
        return ResponseEntity.ok(ApiResponse.success("검색 결과입니다.", results));
    }
}
