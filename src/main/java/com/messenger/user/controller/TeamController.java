package com.messenger.user.controller;

import com.messenger.common.dto.ApiResponse;
import com.messenger.user.dto.TeamResponse;
import com.messenger.user.entity.TeamRole;
import com.messenger.user.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 * TeamController - 팀 관리 REST API 컨트롤러
 * ============================================================
 *
 * 【엔드포인트 목록】
 * GET  /api/teams            → 전체 팀 목록 조회
 * POST /api/teams            → 팀 생성
 * POST /api/teams/{id}/members → 팀에 멤버 추가
 * ============================================================
 */
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /** 전체 팀 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams() {
        List<TeamResponse> teams = teamService.findAllTeams();
        return ResponseEntity.ok(ApiResponse.success("팀 목록을 조회했습니다.", teams));
    }

    /** 팀 생성 */
    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @RequestParam String name,
            @RequestParam(required = false) String description) {

        TeamResponse team = teamService.createTeam(name, description);
        return ResponseEntity.ok(ApiResponse.success("팀이 생성되었습니다.", team));
    }

    /** 팀에 멤버 추가 */
    @PostMapping("/{teamId}/members")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable Long teamId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "MEMBER") TeamRole role) {

        teamService.addMember(teamId, userId, role);
        return ResponseEntity.ok(ApiResponse.success("팀에 멤버가 추가되었습니다."));
    }
}
