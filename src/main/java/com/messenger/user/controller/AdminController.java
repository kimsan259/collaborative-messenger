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
 * AdminController - ?온?귐딆쁽 ??륁뵠筌왖 ?뚢뫂?껅에?살쑎
 * ============================================================
 *
 * ?癒?퓦??쀫７?紐낅뱜 筌뤴뫖以?? * GET    /admin                    ???온?귐딆쁽 ??륁뵠筌왖 (Thymeleaf)
 * GET    /api/admin/users          ???袁⑷퍥 ?????筌뤴뫖以?API
 * PUT    /api/admin/users/{id}/role   ???????亦낅슦釉?癰궰野?API
 * PUT    /api/admin/users/{id}/status ????????怨밴묶 癰궰野?API
 * DELETE /api/admin/users/{id}     ???????????API
 * ============================================================
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    /** ?온?귐딆쁽 ??륁뵠筌왖 */
    @GetMapping("/admin")
    public String adminPage(HttpSession session, Model model) {
        checkAdmin(session);
        model.addAttribute("userId", session.getAttribute("userId"));
        model.addAttribute("displayName", session.getAttribute("displayName"));
        return "admin";
    }

    /** ?袁⑷퍥 ?????筌뤴뫖以?鈺곌퀬??*/
    @GetMapping("/api/admin/users")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(HttpSession session) {
        checkAdmin(session);
        List<UserResponse> users = userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("?????筌뤴뫖以??鈺곌퀬???됰뮸??덈뼄.", users));
    }

    /** ?????亦낅슦釉?癰궰野?*/
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

        log.info("[?온?귐딆쁽] ?????亦낅슦釉?癰궰野?- userId={}, newRole={}", userId, newRole);
        return ResponseEntity.ok(ApiResponse.success("亦낅슦釉??癰궰野껋럥由??됰뮸??덈뼄.", UserResponse.from(user)));
    }

    /** ??????怨밴묶 癰궰野?*/
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

        log.info("[?온?귐딆쁽] ??????怨밴묶 癰궰野?- userId={}, newStatus={}", userId, newStatus);
        return ResponseEntity.ok(ApiResponse.success("?怨밴묶揶쎛 癰궰野껋럥由??됰뮸??덈뼄.", UserResponse.from(user)));
    }

    /** ?????????*/
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

        log.info("[?온?귐딆쁽] ?????????- userId={}", userId);
        return ResponseEntity.ok(ApiResponse.success("????癒? ?????뤿???щ빍??"));
    }

    /** ?온?귐딆쁽 亦낅슦釉?筌ｋ똾寃?*/
    private void checkAdmin(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        if (user.getRole() != UserRole.ADMIN || !user.isActive()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        session.setAttribute("role", user.getRole().name());
    }
}

