package com.messenger.user.controller;

import com.messenger.common.dto.ApiResponse;
import com.messenger.user.dto.UserResponse;
import com.messenger.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // ===== 페이지 요청 =====

    /** 프로필 페이지 */
    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/auth/login";
        UserResponse user = userService.findById(userId);
        model.addAttribute("user", user);
        model.addAttribute("displayName", session.getAttribute("displayName"));
        return "profile";
    }

    // ===== REST API =====

    /** 전체 사용자 목록 조회 */
    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userService.findAllUsers();
        return ResponseEntity.ok(ApiResponse.success("사용자 목록을 조회했습니다.", users));
    }

    /** 특정 사용자 조회 */
    @GetMapping("/api/users/{userId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long userId) {
        UserResponse user = userService.findById(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 정보를 조회했습니다.", user));
    }

    /** 프로필 수정 API */
    @PutMapping("/api/users/profile")
    @ResponseBody
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @RequestBody Map<String, String> request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String displayName = request.get("displayName");
        String email = request.get("email");
        UserResponse updated = userService.updateProfile(userId, displayName, email);
        session.setAttribute("displayName", displayName);
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다.", updated));
    }

    /** 비밀번호 변경 API */
    @PutMapping("/api/users/password")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody Map<String, String> request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        userService.changePassword(userId, request.get("currentPassword"), request.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다."));
    }

    /** 프로필 이미지 업로드 API */
    @PostMapping("/api/users/profile-image")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfileImage(
            @RequestParam("file") MultipartFile file, HttpSession session) throws IOException {
        Long userId = (Long) session.getAttribute("userId");

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("이미지 파일만 업로드 가능합니다. (jpg, png, gif, webp)"));
        }

        // 파일 크기 검증 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("파일 크기는 5MB 이하여야 합니다."));
        }

        // 저장 디렉토리 생성 (절대 경로로 변환)
        Path uploadPath = Paths.get(uploadDir, "profiles").toAbsolutePath();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 고유 파일명 생성
        String filename = UUID.randomUUID().toString() + ext;
        Path filePath = uploadPath.resolve(filename);
        file.transferTo(filePath.toFile());

        // DB 업데이트
        String imageUrl = "/uploads/profiles/" + filename;
        userService.updateProfileImage(userId, imageUrl);

        return ResponseEntity.ok(ApiResponse.success("프로필 이미지가 업로드되었습니다.",
                Map.of("profileImage", imageUrl)));
    }
}
