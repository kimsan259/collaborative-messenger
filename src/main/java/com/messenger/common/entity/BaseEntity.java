package com.messenger.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ============================================================
 * BaseEntity - 모든 엔티티의 부모 클래스
 * ============================================================
 *
 * 【역할】
 * 모든 데이터베이스 테이블에 공통으로 필요한 "생성일시"와 "수정일시"를
 * 자동으로 관리해주는 부모 클래스입니다.
 *
 * 【사용법】
 * 다른 엔티티 클래스에서 이 클래스를 상속(extends)하면,
 * createdAt과 updatedAt이 자동으로 추가됩니다.
 *
 * 예시: public class User extends BaseEntity { ... }
 *
 * 【동작 원리】
 * 1. @MappedSuperclass: 이 클래스 자체는 테이블이 아니고, 필드만 자식에게 상속
 * 2. @EntityListeners(AuditingEntityListener.class): JPA가 엔티티 저장/수정 시 자동으로 시각 기록
 * 3. @CreatedDate: 엔티티가 처음 저장될 때 현재 시각 자동 입력
 * 4. @LastModifiedDate: 엔티티가 수정될 때마다 현재 시각 자동 갱신
 *
 * 【주의사항】
 * 이 기능이 동작하려면 메인 클래스에 @EnableJpaAuditing 어노테이션이 필요합니다.
 * (CollaborativeMessengerApplication.java에 설정되어 있음)
 * ============================================================
 */
@Getter                                          // Lombok: getter 메서드 자동 생성
@MappedSuperclass                                // JPA: 이 클래스의 필드를 자식 엔티티 테이블에 포함
@EntityListeners(AuditingEntityListener.class)   // JPA: 엔티티 변경 이벤트를 감지하여 날짜 자동 기록
public abstract class BaseEntity {

    /**
     * 데이터가 처음 생성(INSERT)된 시각.
     * 한번 저장되면 변경되지 않습니다. (updatable = false)
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 데이터가 마지막으로 수정(UPDATE)된 시각.
     * 엔티티가 수정될 때마다 자동으로 현재 시각으로 갱신됩니다.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
