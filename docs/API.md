# Voyage API 명세서 (프론트엔드용)

Voyage 백엔드 REST API 레퍼런스입니다. 서버를 실행하면 아래 두 곳에서 **항상 최신** 스펙을 볼 수 있습니다.

- **Swagger UI**: `http://localhost:8080/swagger-ui.html` (브라우저에서 직접 호출·테스트 가능)
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs` (Postman·클라이언트 코드 생성기 등에 import)

이 문서는 그 요약본입니다. 스펙이 코드와 다르면 위 OpenAPI가 정본입니다.

---

## 1. 공통 규약

- **Base URL**: `http://localhost:8080` (개발). 모든 경로는 `/api`로 시작(공개 공유 조회 포함).
- **형식**: 요청/응답 본문은 모두 JSON(UTF-8). 파일 업로드만 `multipart/form-data`.
- **인증**: 보호 API는 `Authorization: Bearer <accessToken>` 헤더 필요.
- **시간**: 타임스탬프는 UTC ISO-8601(`2026-08-14T10:00:00Z`). 날짜는 `yyyy-MM-dd`, 시각은 `HH:mm:ss`.
- **금액**: 정수 **최소 단위(minor)**. 예) KRW는 원 단위(₩1,000 → `1000`), USD는 센트(**$10.00 → `1000`**).
- **null 필드**: 응답에서 값이 없는 필드는 생략될 수 있음(`non_null` 직렬화).

### 인증 없이 접근 가능한 공개 엔드포인트
`/api/auth/**`, `/api/share/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/health` — 나머지는 모두 Bearer 토큰 필요.

### 표준 에러 응답
모든 실패는 아래 형태로 반환됩니다.
```json
{
  "code": "C001",
  "message": "잘못된 입력입니다.",
  "errors": [ { "field": "endsOn", "reason": "종료일은 시작일 이후여야 합니다." } ],
  "timestamp": "2026-09-05T10:00:00Z"
}
```
`errors`는 입력 검증 실패(400)일 때만 채워집니다.

| code | HTTP | 의미 |
| --- | --- | --- |
| C001 | 400 | 잘못된 입력(검증 실패) |
| C002 | 401 | 인증 필요 |
| C003 | 403 | 권한 없음 |
| C004 | 404 | 리소스 없음 |
| C005 | 409 | 충돌(낙관적 잠금 등) |
| A001 | 409 | 이미 사용 중인 이메일 |
| A002 | 401 | 이메일/비밀번호 불일치 |
| A003 | 401 | 유효하지 않은 토큰 |
| A004 | 503 | 구글 로그인 미구성 |
| M001 | 400 | 유효하지 않거나 만료된 초대 |
| M002 | 400 | 허용되지 않은 역할 |
| M003 | 403 | 소유자는 변경/제거 불가 |
| M004 | 404 | 멤버 없음 |
| E001 | 400 | 분할 합계 ≠ 지출액 |
| E002 | 400 | 지출 정보 오류(비멤버 등) |
| E003 | 400 | 환율 정보 없음 |
| P001 | 400 | 마감된 투표 |
| P002 | 400 | 유효하지 않은 투표 요청 |
| P003 | 400 | 투표 정보 오류 |
| S001 | 404 | 유효하지 않거나 만료된 공유 링크 |
| S002 | 401 | 공유 비밀번호 필요/불일치 |
| F001 | 400 | 허용되지 않은 파일/크기 초과 |

### 공통 열거형(enum)
- **TripStatus**: `PLANNED` · `ONGOING` · `COMPLETED` · `ARCHIVED`
- **TripRole**: `OWNER` · `EDITOR` · `VIEWER`
- **MemberStatus**: `ACTIVE` · `REMOVED`
- **PlaceStatus**: `WISH`(가고 싶음) · `CONSIDERING`(고려 중) · `CONFIRMED`(확정)
- **SplitMethod**: `EQUAL`(균등) · `RATIO`(비율) · `EXACT`(직접 금액)

### 권한 요약
- **소유자(OWNER)**: 여행 수정/삭제/상태변경, 멤버 관리(초대·역할·제거), 공유 링크.
- **편집자(EDITOR)**: 일정·장소·경비·투표 생성/수정.
- **보기전용(VIEWER)**: 조회 + 투표 참여.
- 비멤버가 여행 하위 리소스에 접근하면 **404**(존재 은폐), 멤버지만 권한이 부족하면 **403**.

---

## 2. 인증 (Auth)

### POST `/api/auth/signup` — 회원가입 · 공개
요청:
```json
{ "email": "minji@voyage.com", "password": "password1", "name": "민지",
  "defaultCurrency": "KRW", "timezone": "Asia/Seoul" }
```
- `email`(필수, 형식), `password`(필수, 8~72자), `name`(필수, ≤100), `defaultCurrency`(선택, 3자, 기본 KRW), `timezone`(선택, 기본 Asia/Seoul).
- **201** → `UserResponse`

### POST `/api/auth/login` — 로그인 · 공개
요청: `{ "email": "...", "password": "..." }` → **200** `TokenResponse`

### POST `/api/auth/refresh` — 토큰 재발급 · 공개
요청: `{ "refreshToken": "..." }` → **200** `TokenResponse`
- 재발급 시 기존 refresh 토큰은 폐기되고 **새 refresh 토큰**이 발급됩니다(회전). 이전 토큰 재사용 시 401(A003).

### POST `/api/auth/logout` — 로그아웃 · 공개
요청: `{ "refreshToken": "..." }` → **204** (해당 refresh 토큰 폐기)

### POST `/api/auth/google` — 구글 로그인 · 공개
요청: `{ "idToken": "<Google ID token>" }` → **200** `TokenResponse`
- 클라이언트가 Google Sign-In으로 받은 ID 토큰을 전달하면 서버가 검증 후 우리 토큰을 발급(없으면 자동 가입).
- 서버에 구글 client-id 미구성 시 503(A004), 유효하지 않은 토큰 401(A003).

### GET `/api/users/me` — 내 프로필 · 인증
**200** → `UserResponse`

**UserResponse**
```json
{ "id": 1, "email": "minji@voyage.com", "name": "민지",
  "avatarUrl": null, "defaultCurrency": "KRW", "timezone": "Asia/Seoul" }
```
**TokenResponse**
```json
{ "tokenType": "Bearer", "accessToken": "<JWT>",
  "accessTokenExpiresIn": 900, "refreshToken": "<opaque>" }
```
> 클라이언트 저장 전략: accessToken은 메모리, refreshToken은 안전한 저장소. 401(A003)을 받으면 `/api/auth/refresh`로 재발급.

---

## 3. 여행 (Trips)

| 메서드 · 경로 | 설명 | 권한 |
| --- | --- | --- |
| POST `/api/trips` | 생성(생성자 자동 OWNER) | 인증 |
| GET `/api/trips?status=` | 내 여행 목록(`status` 선택 필터) | 멤버 |
| GET `/api/trips/{tripId}` | 상세 | 멤버 |
| PATCH `/api/trips/{tripId}` | 부분 수정 | 소유자 |
| PATCH `/api/trips/{tripId}/status` | 상태 변경 | 소유자 |
| DELETE `/api/trips/{tripId}` | 삭제 | 소유자 |

생성 요청:
```json
{ "title": "제주 여름 여행", "destination": "제주", "startsOn": "2026-08-14",
  "endsOn": "2026-08-17", "baseCurrency": "KRW", "timezone": "Asia/Seoul",
  "coverImageUrl": null }
```
- `endsOn`은 `startsOn` 이후여야 함(아니면 400 C001).
- 상태 변경 요청: `{ "status": "ONGOING" }` (사용자가 직접 통제. 날짜로 자동 변경하지 않음).

**TripResponse**
```json
{ "id": 1, "ownerId": 1, "title": "제주 여름 여행", "destination": "제주",
  "startsOn": "2026-08-14", "endsOn": "2026-08-17", "baseCurrency": "KRW",
  "timezone": "Asia/Seoul", "status": "PLANNED", "coverImageUrl": null,
  "myRole": "OWNER", "memberCount": 1,
  "createdAt": "2026-09-05T10:00:00Z", "updatedAt": "2026-09-05T10:00:00Z" }
```

---

## 4. 멤버 · 초대 (Members)

| 메서드 · 경로 | 설명 | 권한 |
| --- | --- | --- |
| GET `/api/trips/{tripId}/members` | 멤버 목록 | 멤버 |
| POST `/api/trips/{tripId}/members` | 초대 생성(토큰 반환) | 소유자 |
| PATCH `/api/trips/{tripId}/members/{userId}` | 역할 변경 | 소유자 |
| DELETE `/api/trips/{tripId}/members/{userId}` | 멤버 제거 | 소유자 |
| GET `/api/trips/{tripId}/invitations` | 대기 초대 목록 | 소유자 |
| DELETE `/api/trips/{tripId}/invitations/{invitationId}` | 초대 폐기 | 소유자 |
| POST `/api/invitations/accept` | 초대 수락 | 인증 |

- 초대 생성 요청: `{ "email": "guest@voyage.com", "role": "EDITOR" }` (`email` 선택, `role`은 EDITOR/VIEWER만).
- **초대 응답에 담긴 `token`은 이때 한 번만 반환**됩니다. 프론트가 초대 링크(예: `https://app/invite?token=...`)를 만들어 공유.
- 수락 요청: `{ "token": "<초대 토큰>" }` → `{ "tripId": 1, "role": "EDITOR" }`. 이미 제거됐던 멤버면 재활성화.
- 역할 변경 요청: `{ "role": "VIEWER" }`. 소유자 역할은 변경/제거 불가(403 M003).

**MemberResponse**: `{ userId, name, email, avatarUrl, role, status, joinedAt }`
**InviteResponse(생성 시)**: `{ invitationId, token, role, email, expiresAt }`
**InvitationResponse(목록)**: `{ id, email, role, status, expiresAt, createdAt }` (token 없음)

---

## 5. 일정 (Itinerary)

| 메서드 · 경로 | 설명 | 권한 |
| --- | --- | --- |
| GET `/api/trips/{tripId}/itinerary` | 일정 목록(날짜·순서 정렬) | 멤버 |
| POST `/api/trips/{tripId}/itinerary` | 일정 생성 | 편집자/소유자 |
| PATCH `/api/itinerary/{itemId}` | 수정(**version 필요**) | 편집자/소유자 |
| DELETE `/api/itinerary/{itemId}` | 삭제 | 편집자/소유자 |
| POST `/api/itinerary/reorder` | 날짜·순서 일괄 변경 | 편집자/소유자 |

- 생성: `{ "date": "2026-08-14", "placeId": 5, "startsAt": "10:00:00", "endsAt": "12:00:00", "transport": "렌터카", "note": "성산일출봉" }` (모두 date 외 선택). `sortOrder`는 서버가 자동 부여.
- 수정: `{ "version": 0, "note": "수정" }` — **version은 필수**. 마지막으로 받은 `version`과 서버 값이 다르면 **409(C005)** → 최신을 다시 조회 후 재시도.
- reorder: `{ "items": [ { "itemId": 12, "date": "2026-08-14", "sortOrder": 0 }, { "itemId": 11, "date": "2026-08-14", "sortOrder": 1 } ] }` → 갱신된 전체 목록 반환.

**ItineraryItemResponse**
```json
{ "id": 12, "tripId": 1, "placeId": 5, "date": "2026-08-14",
  "startsAt": "10:00:00", "endsAt": "12:00:00", "sortOrder": 0,
  "transport": "렌터카", "note": "성산일출봉", "version": 1,
  "createdAt": "...", "updatedAt": "..." }
```

---

## 6. 장소 (Places)

| 메서드 · 경로 | 설명 | 권한 |
| --- | --- | --- |
| GET `/api/trips/{tripId}/places/search?query=` | 외부 검색(현재 stub) | 멤버 |
| GET `/api/trips/{tripId}/places?status=&category=&tag=` | 저장 목록 + 필터 | 멤버 |
| POST `/api/trips/{tripId}/places` | 장소 저장(중복 방지) | 편집자/소유자 |
| PATCH `/api/trips/{tripId}/places/{placeId}` | 수정 | 편집자/소유자 |
| DELETE `/api/trips/{tripId}/places/{placeId}` | 삭제 | 편집자/소유자 |

- 검색 결과(`PlaceSearchResponse`)를 그대로 저장에 전달하면 됩니다. 같은 `providerPlaceId`를 같은 여행에 다시 저장하면 **중복 생성 대신 기존 항목을 반환**.
- 저장 요청: `{ "provider": "STUB", "providerPlaceId": "stub-성산-1", "name": "성산일출봉", "address": "제주...", "latitude": 33.45, "longitude": 126.56, "category": "관광명소", "status": "WISH", "tags": ["일출"], "note": "" }` (name만 필수).
- 수정: `{ "status": "CONFIRMED", "tags": ["일출","명소"] }` (부분 수정; tags는 전체 교체).

**PlaceSearchResponse**: `{ provider, providerPlaceId, name, address, latitude, longitude, category }`
**PlaceResponse**: 위 + `{ id, tripId, status, tags, note, createdAt, updatedAt }`

---

## 7. 경비 · 정산 (Expenses & Settlement)

| 메서드 · 경로 | 설명 | 권한 |
| --- | --- | --- |
| GET `/api/trips/{tripId}/expenses` | 지출 목록 | 멤버 |
| POST `/api/trips/{tripId}/expenses` | 지출 등록 | 편집자/소유자 |
| PATCH `/api/expenses/{expenseId}` | 수정(전체 교체) | 편집자/소유자 |
| DELETE `/api/expenses/{expenseId}` | 삭제 | 편집자/소유자 |
| POST `/api/expenses/{expenseId}/receipt` | 영수증 이미지 업로드(multipart) | 편집자/소유자 |
| GET `/api/trips/{tripId}/settlement` | 순잔액·권장 송금·카테고리 합계 | 멤버 |

지출 등록 요청:
```json
{ "title": "숙소비", "amountMinor": 240000, "currency": "KRW", "category": "숙박",
  "payerId": 1, "splitMethod": "EQUAL", "spentOn": "2026-08-14",
  "receiptUrl": null, "note": null,
  "participants": [ { "userId": 1 }, { "userId": 2 }, { "userId": 3 } ] }
```
- `splitMethod`별 `participants` 규칙:
  - `EQUAL`: `userId`만. 서버가 균등 분배(나머지는 최대잉여법으로 배분).
  - `RATIO`: 각 참여자에 `weight`(양수). 비율대로 배분.
  - `EXACT`: 각 참여자에 `amountMinor`. **합이 지출 `amountMinor`와 정확히 일치**해야 함(아니면 400 E001).
- 결제자·참여자는 모두 해당 여행의 활성 멤버여야 함(아니면 400 E002).
- 다중 통화: `currency`가 여행 기준통화와 다르면 서버가 환율을 조회해 `exchangeRate`·`baseAmountMinor`를 **스냅샷**으로 저장(이후 환율 변동 무영향). 현재 환율은 stub(동일 통화=1).
- 영수증 업로드: `multipart/form-data`, 파트 이름 `file`. jpeg/png/webp, 5MB 이하. 성공 시 갱신된 `ExpenseResponse`(receiptUrl 채워짐) 반환.

**ExpenseResponse**
```json
{ "id": 1, "tripId": 1, "payerId": 1, "title": "숙소비", "amountMinor": 240000,
  "currency": "KRW", "exchangeRate": 1, "baseAmountMinor": 240000, "category": "숙박",
  "splitMethod": "EQUAL", "spentOn": "2026-08-14", "receiptUrl": null, "note": null,
  "splits": [ { "userId": 1, "amountMinor": 80000 }, { "userId": 2, "amountMinor": 80000 },
              { "userId": 3, "amountMinor": 80000 } ],
  "createdAt": "...", "updatedAt": "..." }
```
**SettlementResponse** (모든 금액은 기준통화 minor)
```json
{ "baseCurrency": "KRW", "totalBaseMinor": 330000,
  "balances": [ { "userId": 1, "netMinor": 130000 }, { "userId": 2, "netMinor": -50000 },
                { "userId": 3, "netMinor": -80000 } ],
  "transfers": [ { "fromUserId": 3, "toUserId": 1, "amountMinor": 80000 },
                 { "fromUserId": 2, "toUserId": 1, "amountMinor": 50000 } ],
  "categoryTotals": [ { "category": "숙박", "totalBaseMinor": 240000 } ] }
```
- `netMinor` 양수=받을 금액, 음수=보낼 금액. 모든 balance 합은 0. `transfers`대로 송금하면 전원 0이 됨(실제 송금은 처리하지 않음).

---

## 8. 투표 (Polls)

| 메서드 · 경로 | 설명 | 권한 |
| --- | --- | --- |
| GET `/api/trips/{tripId}/polls` | 투표 목록(결과 포함) | 멤버 |
| POST `/api/trips/{tripId}/polls` | 투표 생성 | 편집자/소유자 |
| GET `/api/polls/{pollId}` | 투표 상세 | 멤버 |
| POST `/api/polls/{pollId}/vote` | 투표/변경 | 멤버(보기전용 포함) |
| DELETE `/api/polls/{pollId}` | 삭제 | 편집자/소유자 |

- 생성: `{ "title": "첫날 저녁?", "multipleChoice": false, "anonymous": false, "closesAt": "2026-08-12T18:00:00Z", "options": ["흑돼지","해산물"] }` (선택지 2개 이상).
- 투표: `{ "optionIds": [10] }` — 기존 표를 **교체**(표 변경). 단일 선택 투표에 복수 전달 시 400(P002). 마감 후 400(P001).

**PollResponse**
```json
{ "id": 1, "tripId": 1, "createdBy": 1, "title": "첫날 저녁?",
  "multipleChoice": false, "anonymous": false, "closesAt": "2026-08-12T18:00:00Z",
  "closed": false,
  "options": [ { "id": 10, "label": "흑돼지", "voteCount": 2, "voterIds": [1,2] },
               { "id": 11, "label": "해산물", "voteCount": 1, "voterIds": [3] } ],
  "totalVoters": 3, "myOptionIds": [10] }
```
- `anonymous`가 true면 각 옵션의 `voterIds`는 `null`(집계 수만 노출).

---

## 9. 알림 · 활동 (Notifications & Activity)

| 메서드 · 경로 | 설명 | 권한 |
| --- | --- | --- |
| GET `/api/notifications?unreadOnly=` | 내 알림 목록 | 인증 |
| GET `/api/notifications/unread-count` | 안 읽은 개수 `{ "count": 3 }` | 인증 |
| PATCH `/api/notifications/{id}/read` | 읽음 처리 | 인증 |
| PATCH `/api/notifications/read-all` | 전체 읽음 | 인증 |
| GET `/api/trips/{tripId}/activity` | 여행 활동 피드 | 멤버 |

**NotificationResponse**: `{ id, type, tripId, message, readAt, read, createdAt }`
**ActivityResponse**: `{ id, tripId, actorId, action, entityType, entityId, message, createdAt }`
- 현재 이벤트 종류: `EXPENSE_CREATED`, `POLL_CREATED`(다른 액션도 동일 패턴으로 확장 예정).

---

## 10. 공유 페이지 (Share)

| 메서드 · 경로 | 설명 | 권한 |
| --- | --- | --- |
| POST `/api/trips/{tripId}/share-links` | 공유 링크 생성 | 소유자 |
| GET `/api/trips/{tripId}/share-links` | 링크 목록 | 소유자 |
| DELETE `/api/trips/{tripId}/share-links/{linkId}` | 링크 폐기 | 소유자 |
| GET `/api/share/{token}?password=` | **공개** 읽기전용 요약 | 공개 |

- 생성: `{ "password": "secret1", "expiresAt": "2026-12-31T00:00:00Z", "includeExpenses": true }` (모두 선택). 응답의 `token`으로 프론트가 공유 URL 생성.
- 공개 조회: 비밀번호가 걸린 링크는 `?password=`로 전달. 틀리거나 없으면 401(S002), 만료/폐기/미존재는 404(S001).

**ShareLinkResponse(생성)**: `{ id, token, includeExpenses, hasPassword, expiresAt, createdAt }` (목록에서는 `token`=null)
**PublicTripSummary**
```json
{ "title": "제주 여름 여행", "destination": "제주", "startsOn": "2026-08-14",
  "endsOn": "2026-08-17", "timezone": "Asia/Seoul", "status": "PLANNED",
  "itinerary": [ { "date": "2026-08-14", "startsAt": "10:00:00", "endsAt": null,
                   "placeName": "성산일출봉", "note": null } ],
  "places": [ { "name": "카페 A", "address": "제주...", "category": "cafe", "status": "WISH" } ],
  "budget": { "baseCurrency": "KRW", "totalBaseMinor": 330000,
              "categoryTotals": [ { "category": "숙박", "totalBaseMinor": 240000 } ] } }
```
- `includeExpenses`가 false면 `budget`은 `null`(생략).

---

## 11. AI 일정 제안 (AI)

### POST `/api/trips/{tripId}/ai/itinerary-drafts` — 인증(멤버)
요청(선택): `{ "preferredCategories": ["관광명소"], "itemsPerDay": 3 }`
- 저장된 장소 + 여행 기간으로 날짜별 초안을 제안. **저장/확정하지 않는 편집 가능한 제안**. 현재는 결정적 stub(추후 실제 LLM 어댑터로 교체).

**ItineraryDraftResponse**
```json
{ "days": [ { "date": "2026-08-14",
              "items": [ { "placeId": 5, "placeName": "성산일출봉", "startsAt": "10:00:00", "note": "AI 추천" } ] } ] }
```

---

## 12. 실시간 (WebSocket / STOMP)

- 엔드포인트: `ws://localhost:8080/ws` (STOMP). 구독: `/topic/trips/{tripId}`.
- 여행에 변화(현재 지출·투표 생성)가 커밋되면 아래 이벤트가 브로드캐스트됩니다.
```json
{ "action": "EXPENSE_CREATED", "tripId": 1, "entityType": "EXPENSE",
  "entityId": 9, "actorId": 2, "title": "숙소비" }
```
- 클라이언트는 이 신호를 받아 해당 리소스를 다시 조회(REST)하면 됩니다.

---

## 13. 오프라인 재시도 (Idempotency)

- 오프라인 큐가 재연결 후 같은 생성 요청을 재전송할 때, POST 요청에 **`Idempotency-Key: <고유값>`** 헤더를 넣으면 서버가 첫 응답을 그대로 재생해 **중복 생성을 방지**합니다(사용자별). 헤더가 없으면 일반 동작.
