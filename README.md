# Collaborative Messenger

> 팀 커뮤니케이션을 위해 설계된 실시간 메신저 플랫폼  
> 채팅, 파일 전송, 온라인 상태, 알림, 업무 리포트를 하나로 통합한 협업 시스템

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?logo=spring)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-010101?logo=socketdotio)
![MySQL](https://img.shields.io/badge/MySQL-Sharding-4479A1?logo=mysql)
![Redis](https://img.shields.io/badge/Redis-Session%2FPresence-DC382D?logo=redis)
![Kafka](https://img.shields.io/badge/Kafka-Event_Driven-231F20?logo=apachekafka)

## 1. 프로젝트 한눈에 보기

`Collaborative Messenger`는 단순 채팅 앱이 아니라,  
실시간 메시징과 협업 데이터를 함께 다루는 **업무형 메신저 백엔드/웹 서비스**입니다.

- WebSocket 기반 실시간 채팅
- 파일/이미지 첨부 메시지
- 친구 온라인/오프라인 상태 확인
- 알림 및 읽음 상태 관리
- 일일 업무 리포트 자동 생성/관리
- Kafka + 샤딩 구조를 활용한 확장형 메시지 처리

## 2. 핵심 기능

### 실시간 채팅
- 채팅방 메시지 실시간 송수신 (STOMP over WebSocket)
- 연결 불안정 시 Polling fallback 동작
- 1:1 / 그룹 채팅방 지원

### 파일/이미지 전송
- 채팅방 파일 업로드 및 다운로드 링크 제공
- 이미지 메시지 썸네일/미리보기 렌더링
- 전송 전 확인 UX 지원(파일명/미리보기/최종 확인)

### 친구/사용자 상태
- 친구 목록 관리
- 온라인/오프라인 상태 확인
- 세션 기반 사용자 인증 상태 유지

### 알림/읽음 처리
- 메시지 도착/읽음 상태 관리
- 사용자별 알림 조회
- 미확인 메시지 카운트 처리

### 업무 리포트
- 일일 리포트 생성 및 조회/수정
- 스케줄링 기반 자동 생성
- 협업 기록 기반 리포트 흐름 확장 가능

## 3. 기술 스택

- Language: `Java 17`
- Framework: `Spring Boot 4`, `Spring MVC`, `Spring Security`, `Spring Data JPA`
- Real-time: `WebSocket`, `STOMP`, `SockJS`
- Frontend (Server-side): `Thymeleaf`, `jQuery`, `Bootstrap`
- Data: `MySQL (Shard 0 / Shard 1)`, `Redis`
- Messaging: `Apache Kafka`, `Zookeeper`
- Infra: `Docker Compose`, `GitHub Actions`

## 4. 아키텍처 흐름

1. 클라이언트가 REST/WebSocket으로 메시지 전송
2. 서버가 Kafka 토픽에 메시지 이벤트 발행
3. Consumer가 메시지를 샤딩 규칙에 따라 MySQL에 저장
4. 채팅방 구독자에게 실시간 브로드캐스트
5. Redis가 세션/접속 상태/캐시를 처리

## 5. 빠른 실행 방법 (로컬)

### 필수 준비
- Docker / Docker Compose
- JDK 17

### 실행
```bash
# 1) 인프라 실행 (MySQL, Redis, Kafka, Zookeeper)
docker compose up -d

# 2) 애플리케이션 실행
./gradlew bootRun
```

### 접속
- 애플리케이션: `http://localhost:8888`

## 6. 프로젝트 구조

```text
src/main/java/com/messenger
  chat/               # 채팅 도메인 (controller/service/repository/entity)
  user/               # 인증, 사용자, 친구, 팀 관리
  notification/       # 알림 도메인
  report/             # 업무 리포트 도메인
  infrastructure/     # websocket/kafka/redis/security/sharding 설정
  common/             # 공통 dto, 예외, 로깅, 유틸
```

## 7. CI/CD 자동화

이 저장소에는 GitHub Actions 워크플로우가 포함되어 있습니다.

- `ci.yml`
  - Push / PR 시 빌드 + 테스트 자동 수행
- `deploy-vps.yml`
  - `main` 브랜치 push 시 VPS 자동 배포

### 배포 시크릿(Repository Secrets)
- `VPS_HOST`
- `VPS_USER`
- `VPS_SSH_KEY`

## 8. 운영 시 권장사항

- 기본 비밀번호/접속정보 즉시 변경
- HTTPS 리버스 프록시(Nginx + Certbot) 적용
- 업로드 파일을 로컬 디스크 대신 Object Storage(S3 호환)로 이전
- 모니터링(Prometheus/Grafana) 및 로그 수집(ELK/Loki) 추가

## 9. 로드맵

- OAuth2 소셜 로그인
- S3 기반 파일 스토리지 분리
- 멀티 노드 WebSocket 스케일링
- Blue/Green 또는 Rolling 배포
- 장애 대응 자동화(헬스체크 + 롤백)

## 10. 라이선스

MIT
