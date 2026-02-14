-- ============================================================
-- MySQL Shard 1 초기화 스크립트
-- ============================================================
-- 이 파일은 Docker 컨테이너가 처음 실행될 때 자동으로 실행됩니다.
-- Shard 1에는 chat_messages 테이블만 존재합니다.
-- (홀수 chatRoomId의 채팅 메시지만 이 DB에 저장됩니다)
--
-- ★ 다른 테이블(users, teams 등)은 Shard 0에만 존재합니다.
-- ★ chat_messages 테이블의 스키마는 Shard 0과 완전히 동일합니다.
-- ============================================================

USE messenger_shard_1;

-- ============================================================
-- 채팅 메시지 테이블 (★ 샤딩 대상 - Shard 0과 동일한 스키마)
-- ============================================================
-- 홀수 chatRoomId의 메시지만 이 테이블에 저장됩니다.
-- 예: chatRoomId = 1, 3, 5, 7 ... → 이 테이블에 저장
--     chatRoomId = 2, 4, 6, 8 ... → Shard 0의 테이블에 저장
--
-- ★ FK를 걸지 않는 이유:
--   users, chat_rooms 테이블이 이 DB에 없으므로 FK 참조가 불가능합니다.
--   대신 애플리케이션 코드에서 데이터 정합성을 보장합니다.
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,                   -- ★ 샤드 키
    sender_id BIGINT NOT NULL,                      -- 메시지 보낸 사용자 ID
    content TEXT NOT NULL,                           -- 메시지 내용
    message_type VARCHAR(20) DEFAULT 'TEXT',         -- 메시지 유형: TEXT, IMAGE, FILE, SYSTEM
    attachment_url VARCHAR(500),                     -- 첨부 파일 URL
    attachment_name VARCHAR(255),                    -- 첨부 파일 원본명
    attachment_content_type VARCHAR(100),            -- 첨부 파일 MIME 타입
    attachment_size BIGINT,                          -- 첨부 파일 크기(byte)
    mentions VARCHAR(500),                           -- 멘션된 사용자 ID 목록
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,      -- 메시지 발송 시각
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_messages_room_sent (chat_room_id, sent_at),
    INDEX idx_messages_sender (sender_id),
    INDEX idx_messages_sent_at (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
