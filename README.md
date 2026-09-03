# Voyage — 공동 여행 플래너 (Backend)

여러 명이 함께 여행을 계획·기록·정산하는 협업 앱 **Voyage**의 백엔드 REST API 서버입니다.
의사결정(장소 후보·투표), 계획(일정), 기록(경비·정산)을 하나의 협업 공간으로 연결합니다.

> 포트폴리오 프로젝트 — 단순 CRUD를 넘어 실무에서 쓰는 구조·검증·테스트·보안 패턴을 지향합니다.

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| 언어 / 런타임 | Java 17 (LTS) |
| 프레임워크 | Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, WebSocket) |
| DB | MySQL 8 |
| 마이그레이션 | Flyway |
| 빌드 | Gradle (Kotlin DSL) + Wrapper |
| DTO 매핑 | MapStruct |
| 인증 | Spring Security + JWT *(예정)* |
| 테스트 | JUnit 5, Testcontainers (실제 MySQL) |
| 문서화 | springdoc-openapi *(예정)* |

## 아키텍처

- 클라이언트(React 웹/PWA, 이후 React Native)는 동일한 REST API와 인증 정책을 공유합니다.
- 백엔드는 도메인별 패키지로 나누고, 각 패키지는 `controller / service / repository / dto` 구조를 따릅니다.

```
com.voyage
 ├─ global            공통 인프라
 │   ├─ config        JPA 감사, 보안 설정
 │   ├─ common        BaseTimeEntity (created/updated 자동 기록)
 │   ├─ exception     전역 예외 처리 + 표준 에러 응답
 │   └─ web           헬스/핑 엔드포인트
 ├─ auth              인증·프로필              (예정)
 ├─ trips             여행·멤버·초대·권한       (예정)
 ├─ itinerary         일정                     (예정)
 ├─ places            장소 탐색·저장            (예정)
 ├─ polls             투표                     (예정)
 ├─ expenses          경비·정산                (예정)
 └─ notifications     알림·활동·실시간          (예정)
```

### 핵심 설계 원칙

- **권한**: `trip_members`를 단일 권한 근거로, 모든 여행 하위 리소스 접근에 멤버십·역할을 강제.
- **금액**: 통화 최소 단위 정수(minor)로 저장해 부동소수점 오차 방지. 지출 시 환율 스냅샷 저장.
- **동시 수정**: 낙관적 잠금(`version`) — 충돌 시 `409 Conflict` + 최신 데이터 반환.
- **시간**: 서버는 UTC 저장, 클라이언트가 여행 시간대로 표시.
- **스키마**: Flyway가 소유 (`ddl-auto=none`).

## 로컬 실행

**요구사항:** JDK 17, 실행 중인 MySQL 8 (또는 Docker).

1. DB 준비 (예시):
   ```sql
   CREATE DATABASE voyage CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'voyage'@'%' IDENTIFIED BY 'voyage';
   GRANT ALL PRIVILEGES ON voyage.* TO 'voyage'@'%';
   ```
2. 환경변수 설정: `.env.example`를 참고 (local 프로필은 기본값 제공).
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

- **단위 테스트**는 Docker 없이 실행됩니다.
- **통합 테스트**는 Testcontainers로 실제 MySQL을 띄워 검증하며, Docker가 없으면 자동으로 건너뜁니다.

## 개발 로드맵

| 단계 | 내용 | 상태 |
| --- | --- | --- |
| 0 | 프로젝트 기반, 프로파일, 예외 처리, 보안 베이스라인 | ✅ |
| 1 | 인증(JWT), 여행 CRUD, 멤버·초대·권한 | ⏳ |
| 2 | 일정 타임라인 CRUD·정렬·이력 | · |
| 3 | 장소 검색·저장·필터 | · |
| 4 | 경비·분할·통계·정산 | · |
| 5 | 투표·알림·실시간(WebSocket) | · |
| 6 | 오프라인·AI 제안·다중 통화·공유 페이지 | · |
