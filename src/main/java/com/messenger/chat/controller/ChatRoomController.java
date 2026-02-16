package com.messenger.chat.controller;

import com.messenger.chat.dto.ChatRoomCreateRequest;
import com.messenger.chat.dto.ChatRoomMemberResponse;
import com.messenger.chat.dto.ChatRoomResponse;
import com.messenger.chat.service.ChatRoomService;
import com.messenger.common.dto.ApiResponse;
import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * ChatRoomController - 채팅방 관리 컨트롤러
 * ============================================================
 */
@Controller
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // ===== 페이지 요청 =====

    /** 채팅방 목록 페이지 */
    @GetMapping("/chat/rooms")
    public String chatRoomsPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            List<ChatRoomResponse> rooms = chatRoomService.findRoomsByUserId(userId);
            model.addAttribute("rooms", rooms);
            model.addAttribute("userId", userId);
            model.addAttribute("displayName", session.getAttribute("displayName"));
        }
        return "chat-rooms";
    }

    /** 채팅방 대화 페이지 (입장 시 읽음 처리) */
    @GetMapping("/chat/room/{roomId}")
    public String chatRoomPage(@PathVariable Long roomId, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId != null) {
            chatRoomService.markAsRead(roomId, userId);
        }

        ChatRoomResponse room = chatRoomService.findRoomById(roomId);
        model.addAttribute("room", room);
        model.addAttribute("userId", session.getAttribute("userId"));
        model.addAttribute("displayName", session.getAttribute("displayName"));
        return "chat";
    }

    // ===== REST API =====

    /** 채팅방 목록 조회 API */
    @GetMapping("/api/chat/rooms")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRooms(HttpSession session) {
        Long userId = requireUserId(session);
        List<ChatRoomResponse> rooms = chatRoomService.findRoomsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("채팅방 목록을 조회했습니다.", rooms));
    }

    /** 채팅방 생성 API */
    @PostMapping("/api/chat/rooms")
    @ResponseBody
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createRoom(
            @Valid @RequestBody ChatRoomCreateRequest request,
            HttpSession session) {
        Long creatorId = requireUserId(session);
        ChatRoomResponse room = chatRoomService.createRoom(request, creatorId);
        return ResponseEntity.ok(ApiResponse.success("채팅방이 생성되었습니다.", room));
    }

    /** 채팅방 읽음 처리 API */
    @PostMapping("/api/chat/rooms/{roomId}/read")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long roomId, HttpSession session) {
        Long userId = requireUserId(session);
        chatRoomService.markAsRead(roomId, userId);
        return ResponseEntity.ok(ApiResponse.success("읽음 처리되었습니다."));
    }

    /** 1:1 DM 채팅방 생성/조회 API */
    @PostMapping("/api/chat/dm/{targetUserId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getOrCreateDm(
            @PathVariable Long targetUserId, HttpSession session) {
        Long currentUserId = requireUserId(session);
        ChatRoomResponse room = chatRoomService.getOrCreateDirectRoom(currentUserId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success("DM 채팅방입니다.", room));
    }

    /** 채팅방 멤버 목록 조회 API */
    @GetMapping("/api/chat/rooms/{roomId}/members")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<ChatRoomMemberResponse>>> getMembers(
            @PathVariable Long roomId) {
        List<ChatRoomMemberResponse> members = chatRoomService.getRoomMembers(roomId);
        return ResponseEntity.ok(ApiResponse.success("멤버 목록을 조회했습니다.", members));
    }

    /** 채팅방에 멤버 초대 API */
    @PostMapping("/api/chat/rooms/{roomId}/members")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> inviteMember(
            @PathVariable Long roomId,
            @RequestBody Map<String, String> request) {
        chatRoomService.inviteMember(roomId, request.get("username"));
        return ResponseEntity.ok(ApiResponse.success("멤버를 초대했습니다."));
    }

    /** 채팅방에서 멤버 강퇴 API */
    @DeleteMapping("/api/chat/rooms/{roomId}/members/{userId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        chatRoomService.removeMember(roomId, userId);
        return ResponseEntity.ok(ApiResponse.success("멤버를 내보냈습니다."));
    }

    /** 채팅방 나가기 API */
    @PostMapping("/api/chat/rooms/{roomId}/leave")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
            @PathVariable Long roomId, HttpSession session) {
        Long userId = requireUserId(session);
        chatRoomService.leaveRoom(roomId, userId);
        return ResponseEntity.ok(ApiResponse.success("채팅방을 나갔습니다."));
    }

    private Long requireUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
