# Voyage — 공동 여행 플래너 (Backend)

여러 명이 함께 여행을 계획·기록·정산하는 협업 앱 **Voyage**의 백엔드 REST API 서버입니다.
의사결정(장소 후보·투표), 계획(일정), 기록(경비·정산)을 하나의 협업 공간으로 연결합니다.

> 포트폴리오 프로젝트 — 단순 CRUD를 넘어 실무에서 쓰는 구조·검증·테스트·보안 패턴을 지향합니다.
> 모든 기능은 기능 브랜치 → PR → **CI(실제 MySQL로 통합 테스트)** 통과 → 머지 흐름으로 개발했습니다.

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| 언어 / 런타임 | Java 17 (LTS) |
| 프레임워크 | Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, WebSocket) |
| DB | MySQL 8 |
| 마이그레이션 | Flyway (V1–V8) |
| 인증 | Spring Security + JWT(JJWT), DB 저장 refresh 토큰(회전) |
| 실시간 | Spring WebSocket + STOMP |
| 빌드 | Gradle (Kotlin DSL) + Wrapper |
| DTO 매핑 | 정적 팩토리 / MapStruct(도입) |
| 테스트 | JUnit 5, Mockito, Testcontainers(실제 MySQL) |
| CI | GitHub Actions |

## 구현 현황

| 단계 | 기능 | 상태 |
| --- | --- | --- |
| 0 | 프로젝트 기반, 프로파일, 전역 예외 처리, 보안 베이스라인, CI | ✅ |
| 1 | 인증(JWT), 여행 CRUD, 멤버 초대·역할·제거 | ✅ |
| 2 | 일정 타임라인 CRUD, 드래그 정렬, 낙관적 잠금 | ✅ |
| 3 | 장소 검색(포트&어댑터)·저장·필터·중복 방지 | ✅ |
| 4 | 경비·분할(균등/비율/직접)·다중 통화·정산 알고리즘 | ✅ |
| 5 | 투표, 알림·활동 피드(이벤트 기반), 실시간(WebSocket) | ✅ |
| 6 | 공유 페이지, AI 일정 제안(stub) | ✅ |
| 후속 | 오프라인 동기화, 영수증 S3, springdoc, Redis 전환, 소셜 로그인 | ⏳ |

## 아키텍처

- 클라이언트(React 웹/PWA, 이후 React Native)는 동일한 REST API와 인증 정책을 공유합니다.
- 백엔드는 도메인별 패키지로 나누고, 각 패키지는 `controller / service / repository / dto` 구조를 따릅니다.

```
com.voyage
 ├─ global         공통 인프라(config, common, exception, util)
 ├─ auth           JWT 인증, refresh 토큰
 ├─ user           사용자 프로필
 ├─ trip           여행·멤버·초대 + TripAccessGuard(권한 공통 모듈)
 ├─ itinerary      날짜별 일정(낙관적 잠금, reorder)
 ├─ place          장소 검색(포트&어댑터)·저장·필터
 ├─ poll           투표·표
 ├─ expense        경비·분할·정산 알고리즘·환율(포트&어댑터)
 ├─ activity       활동 피드·알림(이벤트 기반)
 ├─ realtime       WebSocket/STOMP 브로드캐스트
 ├─ share          공개 읽기전용 공유 링크
 └─ ai             AI 일정 초안(포트&어댑터)
```

### 핵심 설계 원칙

- **권한**: `TripAccessGuard`를 단일 소스로, 모든 여행 하위 리소스에 멤버십·역할을 강제 (비멤버 404, 권한 부족 403).
- **금액**: 통화 최소 단위 정수(minor)로 저장해 부동소수점 오차 방지. 지출 시 환율 스냅샷 저장.
- **정산**: 개인별 순잔액 → 최소 송금 매칭. `ProportionalAllocator`(최대잉여법)로 분할 합계가 정확히 일치, 모든 잔액 합=0 보장.
- **동시 수정**: 낙관적 잠금(`version`) — 충돌 시 `409 Conflict`.
- **알림/실시간**: 도메인 이벤트(`ApplicationEventPublisher`) → 리스너가 활동/알림을 원본 트랜잭션 내 기록, 커밋 후 WebSocket 브로드캐스트.
- **확장 포인트**: 장소 검색·환율·AI는 포트&어댑터로 추상화(현재 stub, 실제 API 어댑터로 교체 가능).
- **시간**: 서버는 UTC 저장, 클라이언트가 여행 시간대로 표시. **스키마는 Flyway가 소유**(`ddl-auto=none`).

## 주요 API

| 도메인 | 대표 엔드포인트 |
| --- | --- |
| 인증 | `POST /api/auth/{signup,login,refresh,logout}`, `GET /api/users/me` |
| 여행 | `POST/GET /api/trips`, `GET/PATCH/DELETE /api/trips/{id}`, `PATCH /api/trips/{id}/status` |
| 멤버 | `POST/GET /api/trips/{id}/members`, `POST /api/invitations/accept`, `PATCH/DELETE /api/trips/{id}/members/{userId}` |
| 일정 | `GET/POST /api/trips/{id}/itinerary`, `PATCH/DELETE /api/itinerary/{itemId}`, `POST /api/itinerary/reorder` |
| 장소 | `GET /api/trips/{id}/places/search`, `GET/POST /api/trips/{id}/places`, `PATCH/DELETE .../{placeId}` |
| 경비·정산 | `GET/POST /api/trips/{id}/expenses`, `PATCH/DELETE /api/expenses/{id}`, `GET /api/trips/{id}/settlement` |
| 투표 | `GET/POST /api/trips/{id}/polls`, `POST /api/polls/{id}/vote` |
| 알림·활동 | `GET/PATCH /api/notifications`, `GET /api/trips/{id}/activity` |
| 공유 | `POST /api/trips/{id}/share-links`, `GET /api/share/{token}`(공개) |
| AI | `POST /api/trips/{id}/ai/itinerary-drafts` |
| 실시간 | STOMP `/ws` → 구독 `/topic/trips/{tripId}` |

## 로컬 실행

**요구사항:** JDK 17, 실행 중인 MySQL 8 (또는 Docker).

1. DB 준비 (예시):
   ```sql
   CREATE DATABASE voyage CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'voyage'@'%' IDENTIFIED BY 'voyage';
   GRANT ALL PRIVILEGES ON voyage.* TO 'voyage'@'%';
   ```
2. 환경변수 설정: `.env.example` 참고 (local 프로필은 기본값 제공).
3. 실행:
   ```bash
   ./gradlew bootRun
   ```
4. 확인:
   ```bash
   curl http://localhost:8080/api/ping
   ```

## 테스트

```bash
./gradlew test
```

- **단위 테스트**는 Docker 없이 실행됩니다(정산·분할 알고리즘, 권한, 검증 등).
- **통합 테스트**는 Testcontainers로 실제 MySQL을 띄워 전체 흐름을 검증하며, Docker가 없으면 자동으로 건너뜁니다. CI(GitHub Actions)에서는 항상 실행됩니다.

## 포트폴리오 시연 시나리오

친구 3명의 제주 여행: 여행 생성 → 초대·수락 → 장소 저장·투표 → 확정 장소를 일정으로 → 서로 다른 결제자·참여자로 경비 3건 → **자동 정산(순잔액·최소 송금)** → 활동 피드/알림 → 공개 공유 링크. 이 흐름은 `ExpenseIntegrationTest` 등 통합 테스트로 검증됩니다.
