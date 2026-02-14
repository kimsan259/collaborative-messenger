-- ============================================================
-- MySQL Shard 0 초기화 스크립트
-- ============================================================
-- 이 파일은 Docker 컨테이너가 처음 실행될 때 자동으로 실행됩니다.
-- Shard 0에는 모든 테이블이 존재합니다:
--   - 공통 테이블: users, teams, user_teams, chat_rooms, chat_room_members
--   - 샤딩 테이블: chat_messages (짝수 chatRoomId의 메시지만 저장)
--   - 업무일지: daily_reports, report_items
--   - 알림: notifications
-- ============================================================

USE messenger_shard_0;

-- ============================================================
-- 1. 사용자 테이블
-- ============================================================
-- 서비스에 가입한 모든 사용자 정보를 저장합니다.
-- username은 로그인에 사용되고, display_name은 채팅방에서 표시됩니다.
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,          -- 사용자 고유 번호 (자동 증가)
    username VARCHAR(50) NOT NULL UNIQUE,           -- 로그인 ID (중복 불가)
    password VARCHAR(255) NOT NULL,                 -- 비밀번호 (BCrypt로 암호화되어 저장)
    display_name VARCHAR(100) NOT NULL,             -- 채팅방에서 보이는 이름
    email VARCHAR(100),                             -- 이메일 주소
    status VARCHAR(20) DEFAULT 'OFFLINE',           -- 접속 상태: ONLINE, OFFLINE, AWAY
    role VARCHAR(20) DEFAULT 'MEMBER',              -- 권한: ADMIN, MANAGER, MEMBER
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 가입 일시
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- 수정 일시
    INDEX idx_users_username (username),             -- 로그인 시 빠른 조회를 위한 인덱스
    INDEX idx_users_status (status)                  -- 온라인 사용자 목록 조회용 인덱스
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. 팀/부서 테이블
-- ============================================================
-- 회사의 팀 또는 부서 정보를 저장합니다.
-- 예: "개발팀", "마케팅팀", "인사팀" 등
-- ============================================================
CREATE TABLE IF NOT EXISTS teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,                     -- 팀 이름
    description TEXT,                                -- 팀 설명
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. 사용자-팀 매핑 테이블 (다대다 관계)
-- ============================================================
-- 한 사용자가 여러 팀에 속할 수 있고, 한 팀에 여러 사용자가 속할 수 있습니다.
-- team_role은 해당 팀에서의 역할(팀장/팀원)입니다.
-- ============================================================
CREATE TABLE IF NOT EXISTS user_teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,                        -- 사용자 ID (users.id 참조)
    team_id BIGINT NOT NULL,                        -- 팀 ID (teams.id 참조)
    team_role VARCHAR(20) DEFAULT 'MEMBER',          -- 팀 내 역할: LEADER(팀장), MEMBER(팀원)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_team (user_id, team_id)       -- 같은 사용자가 같은 팀에 중복 가입 방지
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. 채팅방 테이블
-- ============================================================
-- 1:1 채팅 또는 그룹 채팅을 위한 채팅방 정보를 저장합니다.
-- room_type이 'DIRECT'면 1:1, 'GROUP'이면 그룹 채팅입니다.
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,                     -- 채팅방 이름 (예: "프론트엔드 개발팀 회의")
    room_type VARCHAR(20) NOT NULL,                  -- 채팅방 유형: DIRECT(1:1), GROUP(그룹)
    last_message_id BIGINT,                          -- 마지막 메시지 ID (목록에서 미리보기용)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_chat_rooms_type (room_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. 채팅방 멤버 테이블
-- ============================================================
-- 각 채팅방에 참여한 사용자 목록을 관리합니다.
-- last_read_at은 해당 사용자가 마지막으로 읽은 시각으로, "읽지 않은 메시지 수" 계산에 사용됩니다.
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_room_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,                   -- 채팅방 ID (chat_rooms.id 참조)
    user_id BIGINT NOT NULL,                        -- 사용자 ID (users.id 참조)
    last_read_at DATETIME,                           -- 마지막으로 읽은 시각
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_room_user (chat_room_id, user_id), -- 같은 사용자가 같은 방에 중복 참여 방지
    INDEX idx_member_user (user_id)                   -- 특정 사용자의 채팅방 목록 조회용
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. 채팅 메시지 테이블 (★ 샤딩 대상)
-- ============================================================
-- 모든 채팅 메시지가 저장됩니다. 이 테이블만 2개의 MySQL에 분산 저장(샤딩)됩니다.
-- 샤딩 규칙: chatRoomId % 2 == 0 → 이 테이블 (Shard 0)
--           chatRoomId % 2 == 1 → Shard 1의 동일한 테이블
-- ★ 주의: FK를 걸지 않습니다 (샤드 간 FK는 불가능하므로 값으로만 참조)
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,                   -- ★ 샤드 키: 이 값으로 어떤 MySQL에 저장할지 결정
    sender_id BIGINT NOT NULL,                      -- 메시지 보낸 사용자 ID
    content TEXT NOT NULL,                           -- 메시지 내용
    message_type VARCHAR(20) DEFAULT 'TEXT',         -- 메시지 유형: TEXT, IMAGE, FILE, SYSTEM
    attachment_url VARCHAR(500),                     -- 첨부 파일 URL
    attachment_name VARCHAR(255),                    -- 첨부 파일 원본명
    attachment_content_type VARCHAR(100),            -- 첨부 파일 MIME 타입
    attachment_size BIGINT,                          -- 첨부 파일 크기(byte)
    mentions VARCHAR(500),                           -- 멘션된 사용자 (예: "1,5,12" - 사용자 ID 목록)
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,      -- 메시지 발송 시각
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_messages_room_sent (chat_room_id, sent_at),  -- 채팅방별 메시지 시간순 조회 (가장 많이 쓰는 쿼리)
    INDEX idx_messages_sender (sender_id),                  -- 특정 사용자의 메시지 조회 (업무일지 생성용)
    INDEX idx_messages_sent_at (sent_at)                    -- 날짜별 메시지 조회 (업무일지 생성용)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 7. 일일 업무일지 테이블
-- ============================================================
-- 매일 오후 6시에 자동 생성되는 업무일지입니다.
-- 사용자가 하루 동안 보낸 채팅 메시지를 분석하여 업무 요약을 만듭니다.
-- status로 자동생성/수정됨/확인됨 상태를 관리합니다.
-- ============================================================
CREATE TABLE IF NOT EXISTS daily_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,                        -- 업무일지 대상 사용자
    report_date DATE NOT NULL,                       -- 업무일지 날짜 (예: 2026-02-08)
    summary TEXT,                                    -- 자동 생성된 하루 업무 요약
    total_message_count INT DEFAULT 0,               -- 이 날 보낸 총 메시지 수
    active_room_count INT DEFAULT 0,                 -- 이 날 참여한 활성 채팅방 수
    status VARCHAR(30) DEFAULT 'AUTO_GENERATED',     -- 상태: AUTO_GENERATED, EDITED, CONFIRMED
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_date (user_id, report_date),  -- 같은 날짜에 중복 업무일지 방지
    INDEX idx_reports_date (report_date)              -- 날짜별 조회용 인덱스
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 8. 업무일지 항목 테이블
-- ============================================================
-- 하나의 업무일지에 여러 개의 업무 항목이 포함됩니다.
-- 각 항목은 특정 채팅방에서의 활동을 요약한 것입니다.
-- 예: "프론트엔드 버그 수정 논의 (개발팀 채팅방, 메시지 23건)"
-- ============================================================
CREATE TABLE IF NOT EXISTS report_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    daily_report_id BIGINT NOT NULL,                -- 소속 업무일지 ID
    title VARCHAR(200) NOT NULL,                     -- 업무 항목 제목
    description TEXT,                                -- 상세 내용 (관련 대화 요약)
    chat_room_name VARCHAR(200),                     -- 어떤 채팅방에서 논의했는지
    related_message_count INT DEFAULT 0,             -- 관련 메시지 수
    category VARCHAR(30) DEFAULT 'OTHER',            -- 업무 카테고리: DEVELOPMENT, MEETING, REVIEW, OTHER
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (daily_report_id) REFERENCES daily_reports(id) ON DELETE CASCADE,
    INDEX idx_items_report (daily_report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 9. 알림 테이블
-- ============================================================
-- 사용자에게 전달되는 모든 알림을 저장합니다.
-- 알림 종류: 멘션(@), 업무일지 생성 완료, 채팅방 초대 등
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,                   -- 알림 수신자 ID
    type VARCHAR(30) NOT NULL,                       -- 알림 유형: MENTION, REPORT_GENERATED, ROOM_INVITE
    message VARCHAR(500) NOT NULL,                   -- 알림 내용 (예: "김개발님이 당신을 멘션했습니다")
    reference_id BIGINT,                             -- 관련 엔티티 ID (메시지ID 또는 리포트ID)
    reference_type VARCHAR(30),                      -- 관련 엔티티 종류: CHAT_MESSAGE, DAILY_REPORT
    is_read BOOLEAN DEFAULT FALSE,                   -- 읽음 여부
    read_at DATETIME,                                -- 읽은 시각
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_recipient (recipient_id, is_read),  -- 사용자별 읽지 않은 알림 조회
    INDEX idx_notif_created (created_at)                 -- 최근 알림 조회용
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 10. 친구 관계 테이블
-- ============================================================
-- 사용자 간의 친구 관계를 관리합니다.
-- requester가 receiver에게 친구 요청을 보내는 구조입니다.
-- status: PENDING(대기) → ACCEPTED(수락) / REJECTED(거절) / BLOCKED(차단)
-- ============================================================
CREATE TABLE IF NOT EXISTS friendships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requester_id BIGINT NOT NULL,                  -- 친구 요청을 보낸 사용자
    receiver_id BIGINT NOT NULL,                   -- 친구 요청을 받은 사용자
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 관계 상태: PENDING, ACCEPTED, REJECTED, BLOCKED
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_requester_receiver (requester_id, receiver_id),
    INDEX idx_friendship_receiver (receiver_id, status),
    INDEX idx_friendship_requester (requester_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 초기 테스트 데이터 (개발 편의용)
-- ============================================================
-- 테스트용 팀 2개를 미리 생성합니다.
-- ============================================================
INSERT INTO teams (name, description) VALUES
    ('개발팀', '소프트웨어 개발을 담당하는 팀입니다.'),
    ('마케팅팀', '마케팅 전략 및 캠페인을 담당하는 팀입니다.');
