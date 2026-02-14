package com.messenger.user.entity;

import com.messenger.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * ============================================================
 * UserTeam - 사용자와 팀의 매핑 엔티티 (다대다 관계)
 * ============================================================
 *
 * 【역할】
 * 사용자와 팀의 다대다(N:M) 관계를 중간 테이블로 관리합니다.
 *
 * 【다대다 관계란?】
 * - 한 사용자가 여러 팀에 속할 수 있음 (예: 김개발이 "개발팀"과 "QA팀"에 동시 소속)
 * - 한 팀에 여러 사용자가 속할 수 있음 (예: "개발팀"에 김개발, 이기획, 박디자인 소속)
 *
 * 【왜 @ManyToMany 대신 중간 엔티티를 만드는가?】
 * 1. 팀 내 역할(teamRole: LEADER/MEMBER) 같은 추가 정보를 저장할 수 있음
 * 2. JPA의 @ManyToMany는 중간 테이블을 직접 제어하기 어려움
 * 3. 실무에서도 거의 항상 중간 엔티티를 별도로 만듦
 *
 * 【테이블】 user_teams (Shard 0에 저장)
 * ============================================================
 */
@Entity
@Table(name = "user_teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserTeam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 사용자.
     * @ManyToOne: 여러 UserTeam이 하나의 User를 참조 (N:1 관계)
     * fetch = LAZY: 이 엔티티를 조회할 때 User를 바로 조회하지 않고,
     *               실제로 user를 사용할 때 DB 쿼리를 실행 (성능 최적화)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * 소속 팀.
     * @ManyToOne: 여러 UserTeam이 하나의 Team을 참조 (N:1 관계)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    /** 팀 내 역할: LEADER(팀장), MEMBER(팀원) */
    @Enumerated(EnumType.STRING)
    @Column(name = "team_role", length = 20)
    private TeamRole teamRole;
}
