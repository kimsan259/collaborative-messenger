package com.messenger.user.entity;

import com.messenger.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * ============================================================
 * Team - 팀/부서 엔티티
 * ============================================================
 *
 * 【역할】
 * 회사의 팀 또는 부서 정보를 나타냅니다.
 * 예: "개발팀", "마케팅팀", "인사팀" 등
 *
 * 【테이블】 teams (Shard 0에 저장)
 * ============================================================
 */
@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 팀 이름 (예: "개발팀") */
    @Column(nullable = false, length = 100)
    private String name;

    /** 팀 설명 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 팀 이름을 변경합니다. */
    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
