# Collaborative Messenger

Real-time team messenger built with Spring Boot, WebSocket(STOMP), MySQL sharding, Redis session/presence, and Kafka event processing.

## Highlights
- Real-time chat room messaging with WebSocket + fallback polling
- File and image upload in chat rooms
- Friend online/offline status tracking
- Notification system
- Daily work report generation
- Chat message sharding across MySQL instances

## Tech Stack
- Java 17
- Spring Boot 4
- Spring Security, Spring Data JPA
- WebSocket(STOMP), Thymeleaf, jQuery, Bootstrap
- MySQL (Shard 0 / Shard 1), Redis, Kafka, Zookeeper
- Docker Compose

## Architecture
1. Client sends chat message through REST/WebSocket.
2. Message is published to Kafka topic.
3. Consumer persists message to sharded MySQL.
4. Message is broadcast to subscribers in real time.
5. Presence/session state is managed through Redis.

## Quick Start (Local)
```bash
# 1) infrastructure
docker compose up -d

# 2) app
./gradlew bootRun
```

Application URL: `http://localhost:8888`

## Main Features
- Account registration / login / profile
- Team and friend management
- 1:1 and group chat room
- File/Image attachment message
- Online status indicator
- Read/unread handling
- Notification center
- Daily report view and update

## Project Structure
```text
src/main/java/com/messenger
  chat/               # chat domain (controller/service/repository/entity)
  user/               # auth, user, friend, team
  notification/       # notification domain
  report/             # daily report domain
  infrastructure/     # websocket/kafka/redis/security/sharding
  common/             # shared config, dto, exception, logging
```

## CI/CD (GitHub Actions)
This repository includes:
- `.github/workflows/ci.yml`: build + test on push/PR
- `.github/workflows/deploy-vps.yml`: auto deploy to VPS on `main`

### Required GitHub Secrets for deploy
- `VPS_HOST`
- `VPS_USER`
- `VPS_SSH_KEY`

## One-command Deploy Strategy
`deploy-vps.yml` does:
1. SSH to your server
2. Pull latest `main`
3. Rebuild and restart with Docker Compose

## Roadmap
- Move file storage to S3-compatible object storage
- Add OAuth login
- Add monitoring (Prometheus + Grafana)
- Blue/Green deployment

## License
MIT
