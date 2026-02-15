package com.messenger.user.controller;

import com.messenger.common.dto.ApiResponse;
import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import com.messenger.user.dto.FriendshipResponse;
import com.messenger.user.service.FriendshipService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    private Long requireUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    @GetMapping("/friends")
    public String friendsPage(HttpSession session, Model model) {
        Long userId = requireUserId(session);
        model.addAttribute("userId", userId);
        model.addAttribute("displayName", session.getAttribute("displayName"));
        return "friends";
    }

    @GetMapping("/api/friends")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getFriends(HttpSession session) {
        Long userId = requireUserId(session);
        List<FriendshipResponse> friends = friendshipService.getFriends(userId);
        return ResponseEntity.ok(ApiResponse.success("친구 목록을 조회했습니다.", friends));
    }

    @GetMapping("/api/friends/requests/received")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getReceivedRequests(HttpSession session) {
        Long userId = requireUserId(session);
        List<FriendshipResponse> requests = friendshipService.getPendingReceivedRequests(userId);
        return ResponseEntity.ok(ApiResponse.success("받은 친구 요청을 조회했습니다.", requests));
    }

    @GetMapping("/api/friends/requests/sent")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getSentRequests(HttpSession session) {
        Long userId = requireUserId(session);
        List<FriendshipResponse> requests = friendshipService.getPendingSentRequests(userId);
        return ResponseEntity.ok(ApiResponse.success("보낸 친구 요청을 조회했습니다.", requests));
    }

    @PostMapping("/api/friends/request/{receiverId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<FriendshipResponse>> sendRequest(
            @PathVariable Long receiverId, HttpSession session) {
        Long userId = requireUserId(session);
        FriendshipResponse response = friendshipService.sendFriendRequest(userId, receiverId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청을 보냈습니다.", response));
    }

    @PostMapping("/api/friends/accept/{friendshipId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<FriendshipResponse>> acceptRequest(
            @PathVariable Long friendshipId, HttpSession session) {
        Long userId = requireUserId(session);
        FriendshipResponse response = friendshipService.acceptFriendRequest(friendshipId, userId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청을 수락했습니다.", response));
    }

    @PostMapping("/api/friends/reject/{friendshipId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> rejectRequest(
            @PathVariable Long friendshipId, HttpSession session) {
        Long userId = requireUserId(session);
        friendshipService.rejectFriendRequest(friendshipId, userId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청을 거절했습니다."));
    }

    @DeleteMapping("/api/friends/{friendshipId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> removeFriend(
            @PathVariable Long friendshipId, HttpSession session) {
        Long userId = requireUserId(session);
        friendshipService.removeFriend(friendshipId, userId);
        return ResponseEntity.ok(ApiResponse.success("친구를 삭제했습니다."));
    }

    @GetMapping("/api/friends/search")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> searchUsers(
            @RequestParam String keyword, HttpSession session) {
        Long userId = requireUserId(session);
        List<FriendshipResponse> results = friendshipService.searchUsers(keyword, userId);
        return ResponseEntity.ok(ApiResponse.success("검색 결과입니다.", results));
    }
}
