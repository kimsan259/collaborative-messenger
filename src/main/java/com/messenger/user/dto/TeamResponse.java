package com.messenger.user.dto;

import com.messenger.user.entity.Team;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * ============================================================
 * TeamResponse - 팀 정보 응답 DTO
 * ============================================================
 */
@Getter
@Builder
@AllArgsConstructor
public class TeamResponse {

    private Long id;
    private String name;
    private String description;

    /** Team 엔티티를 TeamResponse DTO로 변환합니다. */
    public static TeamResponse from(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .build();
    }
}
