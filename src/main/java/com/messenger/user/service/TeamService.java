package com.messenger.user.service;

import com.messenger.common.exception.BusinessException;
import com.messenger.common.exception.ErrorCode;
import com.messenger.user.dto.TeamResponse;
import com.messenger.user.entity.Team;
import com.messenger.user.entity.TeamRole;
import com.messenger.user.entity.User;
import com.messenger.user.entity.UserTeam;
import com.messenger.user.repository.TeamRepository;
import com.messenger.user.repository.UserRepository;
import com.messenger.user.repository.UserTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * TeamService - 팀 관리 비즈니스 로직
 * ============================================================
 *
 * 【역할】
 * 팀 생성, 조회, 멤버 추가/제거 등 팀 관련 비즈니스 로직을 처리합니다.
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final UserTeamRepository userTeamRepository;

    /**
     * 【팀 생성】
     */
    @Transactional
    public TeamResponse createTeam(String name, String description) {
        log.info("[팀 생성] 팀이름={}", name);

        Team team = Team.builder()
                .name(name)
                .description(description)
                .build();

        Team savedTeam = teamRepository.save(team);
        log.info("[팀 생성 완료] 팀ID={}, 팀이름={}", savedTeam.getId(), savedTeam.getName());

        return TeamResponse.from(savedTeam);
    }

    /**
     * 【전체 팀 목록 조회】
     */
    public List<TeamResponse> findAllTeams() {
        return teamRepository.findAll().stream()
                .map(TeamResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 【팀에 멤버 추가】
     *
     * @param teamId 팀 ID
     * @param userId 추가할 사용자 ID
     * @param role   팀 내 역할 (LEADER/MEMBER)
     */
    @Transactional
    public void addMember(Long teamId, Long userId, TeamRole role) {
        // 팀 존재 확인
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 이미 팀 멤버인지 확인
        if (userTeamRepository.existsByUserIdAndTeamId(userId, teamId)) {
            throw new BusinessException(ErrorCode.ALREADY_TEAM_MEMBER);
        }

        UserTeam userTeam = UserTeam.builder()
                .user(user)
                .team(team)
                .teamRole(role)
                .build();

        userTeamRepository.save(userTeam);
        log.info("[팀 멤버 추가] 팀={}, 사용자={}, 역할={}", team.getName(), user.getUsername(), role);
    }

    /**
     * 【특정 팀의 멤버 목록 조회】
     */
    public List<UserTeam> findTeamMembers(Long teamId) {
        return userTeamRepository.findByTeamId(teamId);
    }
}
