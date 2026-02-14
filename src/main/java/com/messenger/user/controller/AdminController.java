package com.messenger.user.controller;

import com.messenger.common.dto.ApiResponse;
import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import com.messenger.user.dto.UserResponse;
import com.messenger.user.entity.User;
import com.messenger.user.entity.UserRole;
import com.messenger.user.entity.UserStatus;
import com.messenger.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================
 * AdminController - 관리자 페이지 컨트롤러
 * ============================================================
 *
 * 【엔드포인트 목록】
 * GET    /admin                    → 관리자 페이지 (Thymeleaf)
 * GET    /api/admin/users          → 전체 사용자 목록 API
 * PUT    /api/admin/users/{id}/role   → 사용자 권한 변경 API
 * PUT    /api/admin/users/{id}/status → 사용자 상태 변경 API
 * DELETE /api/admin/users/{id}     → 사용자 삭제 API
 * ============================================================
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    /** 관리자 페이지 */
    @GetMapping("/admin")
    public String adminPage(HttpSession session, Model model) {
        checkAdmin(session);
        model.addAttribute("userId", session.getAttribute("userId"));
        model.addAttribute("displayName", session.getAttribute("displayName"));
        return "admin";
    }

    /** 전체 사용자 목록 조회 */
    @GetMapping("/api/admin/users")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(HttpSession session) {
        checkAdmin(session);
        List<UserResponse> users = userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("사용자 목록을 조회했습니다.", users));
    }

    /** 사용자 권한 변경 */
    @PutMapping("/api/admin/users/{userId}/role")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        checkAdmin(session);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserRole newRole = UserRole.valueOf(body.get("role"));
        user.updateRole(newRole);
        userRepository.save(user);

        log.info("[관리자] 사용자 권한 변경 - userId={}, newRole={}", userId, newRole);
        return ResponseEntity.ok(ApiResponse.success("권한이 변경되었습니다.", UserResponse.from(user)));
    }

    /** 사용자 상태 변경 */
    @PutMapping("/api/admin/users/{userId}/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        checkAdmin(session);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserStatus newStatus = UserStatus.valueOf(body.get("status"));
        user.updateStatus(newStatus);
        userRepository.save(user);

        log.info("[관리자] 사용자 상태 변경 - userId={}, newStatus={}", userId, newStatus);
        return ResponseEntity.ok(ApiResponse.success("상태가 변경되었습니다.", UserResponse.from(user)));
    }

    /** 사용자 삭제 */
    @DeleteMapping("/api/admin/users/{userId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId, HttpSession session) {
        checkAdmin(session);

        Long currentUserId = (Long) session.getAttribute("userId");
        if (currentUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userRepository.deleteById(userId);

        log.info("[관리자] 사용자 삭제 - userId={}", userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 삭제되었습니다."));
    }

    /** 관리자 권한 체크 */
    private void checkAdmin(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null || !role.equals("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
