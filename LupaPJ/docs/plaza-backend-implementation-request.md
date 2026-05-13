# 광장 API 수정 및 추가 구현 요청서 - MVP 범위 기준

## 요청 목적

이 문서는 프론트엔드에서 구현한 광장 화면 및 `PlazaRepository` 구조를 실제 백엔드 API와 연동하기 위한 MVP 기준 요청서이다.

이번 MVP 범위에서는 광장 입장, 광장 코드 입장, 현재 광장 조회, 광장 스냅샷 조회, 광장 퇴장, 참가자/펫 표시, 채팅 기능을 구현 대상으로 한다.

펫 상호작용, WebSocket 실시간 동기화, 참가자 이동 명령, 서버 tick 처리는 MVP 필수 범위에서 제외하고 후순위 확장 기능으로 분리한다.

## 현재 구현 현황

### 현재 구현된 점

| 구분 | 내용 |
|---|---|
| 백엔드 API prefix | `/plazas`로 구성되어 있음 |
| 랜덤 광장 입장 | `POST /plazas/random/join` 구현되어 있음 |
| 코드 광장 입장 | `POST /plazas/code/join` 구현되어 있음 |
| 현재 광장 조회 | `GET /plazas/me/active` 구현되어 있음 |
| 인증 사용자 식별 | `@RequestAttribute("currentUid")`로 현재 사용자 식별 |
| 광장 생성 | 입장 가능한 광장이 없으면 새 광장 생성 |
| 광장 코드 | 광장 생성 시 코드 생성, 코드 입장 시 입력값 정규화 처리 |
| 최대 인원 | 광장 정원 4명 기준 처리 |
| 참가자/펫 응답 | 참가자 정보와 펫 스냅샷 일부 응답 가능 |
| 프론트 광장 화면 | 광장 진입 화면, 내부 화면, 코드 표시, 펫 표시, 채팅 UI 구현 |
| 프론트 구조 | `PlazaRepository`, `MockPlazaRepository`, 광장 상태 모델 구현 |

### MVP 기준 수정 및 추가가 필요한 점

| 구분 | 필요한 작업 |
|---|---|
| 응답 DTO 정리 | MVP에서 필요한 필드만 남겨 `PlazaRoomResponse` 구조 단순화 |
| 에러 응답 정리 | 광장 예외가 `500 UNKNOWN`으로 내려가지 않도록 에러 코드/status 정리 |
| 광장 스냅샷 조회 | `GET /plazas/{plazaId}` 추가 |
| 광장 퇴장 | `POST /plazas/{plazaId}/leave` 추가 |
| 광장 채팅 전송 | `POST /plazas/{plazaId}/messages` 추가 |
| 메시지 응답 | 최근 채팅 메시지를 `PlazaChatMessageResponse[]`로 내려주도록 수정 |
| 참가자 검증 | 스냅샷 조회, 퇴장, 채팅 전송 시 현재 사용자가 해당 광장 참가자인지 검증 |
| 프론트 실제 연동 | 백엔드 API 확정 후 프론트에서 `RemotePlazaRepository`로 연결 |

## 구현 우선순위

### MVP 1차 필수 구현

- `POST /plazas/random/join`
- `POST /plazas/code/join`
- `GET /plazas/me/active`
- `GET /plazas/{plazaId}`
- `POST /plazas/{plazaId}/leave`
- `POST /plazas/{plazaId}/messages`
- 광장 참가자 목록 응답
- 참가자별 펫 스냅샷 응답
- 최근 채팅 메시지 응답
- 기본 에러 코드 정리

### 후순위 확장 기능

아래 항목은 MVP 필수 구현 범위에서 제외한다.

- `POST /plazas/{plazaId}/interactions`
- `PlazaInteractionEvent`
- `WebSocket /ws/plazas/{plazaId}`
- `PLAZA_SNAPSHOT` broadcast
- `PlazaMovementCommand`
- `movement` 관련 필드
- `positionUpdatedAtMillis`
- 서버 tick / scheduled job
- `serverTime` 기반 실시간 보정
- `isServerAuthoritative`
- 상호작용/이동/실시간 연결 전용 에러 코드

## MVP 서버 처리 원칙

- 사용자는 동시에 하나의 광장에만 참여할 수 있다.
- 광장 코드는 코드 입장 및 초대 공유에 사용한다.
- 광장 참가자 수는 `maxParticipants`를 초과할 수 없다.
- 광장 입장 시 참가자의 `position`은 서버에서 기본값 또는 랜덤값으로 지정한다.
- MVP에서는 목적지 이동 처리나 이동 애니메이션 계산을 서버에서 수행하지 않는다.
- 광장 퇴장 시 참가자 목록에서 현재 사용자를 제거한다.
- 참가자가 0명이 된 광장은 삭제할 수 있다.
- 채팅 메시지는 최근 N개만 응답해도 된다. 권장값은 최근 50개이다.
- MVP에서는 WebSocket 없이 REST API 조회 방식으로 상태를 갱신할 수 있다.
- `GET /plazas/{plazaId}`는 새로고침 또는 polling 용도로 사용할 수 있어야 한다.

## API 계약

### 1. 랜덤 광장 입장

```http
POST /plazas/random/join
Authorization: Bearer {accessToken}
Content-Type: application/json
```

Request body:

```json
{}
```

처리 기준:

- 현재 사용자가 활성 광장에 이미 참여 중이면 기존 활성 광장을 반환한다.
- 활성 광장이 없다면 정원이 남은 광장에 입장한다.
- 정원이 남은 광장이 없다면 새 광장을 생성한 뒤 입장한다.
- 응답은 `PlazaRoomResponse` 기준으로 반환한다.

Response:

```json
{
  "plaza": {
    "plazaId": "plaza_001",
    "plazaCode": "PZ8K21",
    "displayPlazaCode": "PZ-8K21",
    "participants": [],
    "messages": [],
    "maxParticipants": 4,
    "roomRevision": 1
  }
}
```

### 2. 광장 코드 입장

```http
POST /plazas/code/join
Authorization: Bearer {accessToken}
Content-Type: application/json
```

Request body:

```json
{
  "code": "PZ8K21"
}
```

처리 기준:

- `PZ-8K21`, `PZ8K21`, `8K21` 형식의 입력을 허용한다.
- 서버는 입력 코드를 하이픈 없는 대문자 형식으로 정규화한다.
- 존재하지 않는 코드는 `PLAZA_NOT_FOUND`를 반환한다.
- 코드 형식이 올바르지 않으면 `INVALID_PLAZA_CODE`를 반환한다.
- 정원이 가득 찬 광장은 `PLAZA_FULL`을 반환한다.
- 현재 사용자가 이미 같은 광장에 참여 중이면 기존 광장 정보를 반환한다.
- 현재 사용자가 다른 광장에 참여 중이고 코드 입장에 성공하면 기존 광장 참가자 목록에서 제거한 뒤 새 광장에 입장시킨다.
- 코드 입장에 실패하면 기존 광장 상태는 유지한다.
- 응답은 `PlazaRoomResponse` 기준으로 반환한다.

Response:

```json
{
  "plaza": {
    "plazaId": "plaza_001",
    "plazaCode": "PZ8K21",
    "displayPlazaCode": "PZ-8K21",
    "participants": [],
    "messages": [],
    "maxParticipants": 4,
    "roomRevision": 2
  }
}
```

### 3. 현재 광장 조회

```http
GET /plazas/me/active
Authorization: Bearer {accessToken}
```

처리 기준:

- 현재 사용자가 참여 중인 활성 광장이 있으면 `PlazaRoomResponse`를 반환한다.
- 참여 중인 광장이 없으면 `200 OK`와 함께 `{ "plaza": null }`을 반환한다.

Response:

```json
{
  "plaza": {
    "plazaId": "plaza_001",
    "plazaCode": "PZ8K21",
    "displayPlazaCode": "PZ-8K21",
    "participants": [],
    "messages": [],
    "maxParticipants": 4,
    "roomRevision": 3
  }
}
```

참여 중인 광장이 없는 경우:

```json
{
  "plaza": null
}
```

### 4. 광장 스냅샷 조회

```http
GET /plazas/{plazaId}
Authorization: Bearer {accessToken}
```

처리 기준:

- 특정 광장의 현재 상태를 조회한다.
- 현재 사용자가 해당 광장 참가자인지 확인한다.
- 참가자가 아니면 `NOT_IN_PLAZA`를 반환한다.
- 존재하지 않는 광장이면 `PLAZA_NOT_FOUND`를 반환한다.
- 응답에는 참가자 목록, 참가자별 펫 스냅샷, 최근 채팅 메시지를 포함한다.
- MVP에서는 WebSocket 대신 이 API를 통해 새로고침 또는 polling 방식으로 상태를 갱신할 수 있다.

Response:

```json
{
  "plaza": {
    "plazaId": "plaza_001",
    "plazaCode": "PZ8K21",
    "displayPlazaCode": "PZ-8K21",
    "participants": [],
    "messages": [],
    "maxParticipants": 4,
    "roomRevision": 4
  }
}
```

### 5. 광장 퇴장

```http
POST /plazas/{plazaId}/leave
Authorization: Bearer {accessToken}
```

처리 기준:

- 현재 사용자가 해당 광장 참가자인지 확인한다.
- 참가자가 아니면 `NOT_IN_PLAZA`를 반환한다.
- 현재 사용자를 광장 참가자 목록에서 제거한다.
- 퇴장 후 참가자 수가 0명이면 광장을 삭제할 수 있다.

Response:

```http
204 No Content
```

### 6. 광장 채팅 전송

```http
POST /plazas/{plazaId}/messages
Authorization: Bearer {accessToken}
Content-Type: application/json
```

Request body:

```json
{
  "text": "안녕!"
}
```

처리 기준:

- 현재 사용자가 해당 광장 참가자인지 확인한다.
- 참가자가 아니면 `NOT_IN_PLAZA`를 반환한다.
- 메시지는 trim 후 저장한다.
- 빈 메시지는 `EMPTY_MESSAGE`를 반환한다.
- 120자를 초과하면 `MESSAGE_TOO_LONG`을 반환한다.
- 성공 시 생성된 `PlazaChatMessageResponse`를 반환한다.
- `roomRevision`을 관리하는 경우 메시지 저장 후 증가시켜도 된다.

Response:

```json
{
  "message": {
    "id": "plaza_message_001",
    "senderUserId": "user_me",
    "senderNickname": "나의루파",
    "text": "안녕!",
    "sentAtMillis": 1778509810000
  },
  "roomRevision": 5
}
```

## MVP 응답 DTO

### PlazaRoomResponse

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `plazaId` | `String` | O | 광장 ID |
| `plazaCode` | `String` | O | 공유용 광장 코드. 예: `PZ8K21` |
| `displayPlazaCode` | `String` | 선택 | 화면 표시용 코드. 프론트에서 포맷 처리 가능 |
| `participants` | `List<PlazaParticipantResponse>` | O | 현재 광장 참가자 목록 |
| `messages` | `List<PlazaChatMessageResponse>` | O | 최근 채팅 메시지 목록 |
| `maxParticipants` | `Int` | O | 광장 최대 인원. MVP 기준 4 |
| `roomRevision` | `Long` | 선택 | 광장 상태 변경 버전. 관리 가능하면 포함 |

예시:

```json
{
  "plazaId": "plaza_001",
  "plazaCode": "PZ8K21",
  "displayPlazaCode": "PZ-8K21",
  "participants": [],
  "messages": [],
  "maxParticipants": 4,
  "roomRevision": 1
}
```

### PlazaParticipantResponse

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `userId` | `String` | O | 참가자 유저 ID |
| `nickname` | `String` | O | 참가자 닉네임 |
| `pet` | `PlazaPetSnapshotResponse` | O | 광장에 표시할 펫 정보 |
| `position` | `PlazaPositionResponse` | O | 광장 내 펫 표시 위치 |
| `joinedAtMillis` | `Long` | 선택 | 입장 시각. 화면에 표시하지 않으면 생략 가능 |

예시:

```json
{
  "userId": "user_me",
  "nickname": "나의루파",
  "pet": {
    "petId": "pet_me",
    "name": "루파",
    "characterAssetKey": "room/characters/lupa_default",
    "appearance": {
      "headSizeScale": 1.0,
      "bodySizeScale": 1.0,
      "eyeSizeScale": 1.0,
      "noseSizeScale": 1.0,
      "mouthSizeScale": 1.0
    }
  },
  "position": {
    "x": 0.42,
    "y": 0.68
  }
}
```

### PlazaPetSnapshotResponse

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `petId` | `String` | O | 펫 ID |
| `name` | `String` | O | 펫 이름 |
| `characterAssetKey` | `String` | O | 펫 이미지/스프라이트 선택용 에셋 키 |
| `appearance` | `PetAppearanceResponse` | O | 펫 외형 배율 값 |

MVP에서는 `ownerUserId`를 제거한다. 참가자 주인은 `PlazaParticipantResponse.userId`로 판단한다.

MVP에서는 `status`, `personality`, `equippedItemIds`를 필수 응답에서 제외한다. 해당 값들은 후순위 확장 필드로 분리한다.

### PetAppearanceResponse

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `headSizeScale` | `Float` | O | 머리 크기 배율 |
| `bodySizeScale` | `Float` | O | 몸 크기 배율 |
| `eyeSizeScale` | `Float` | O | 눈 크기 배율 |
| `noseSizeScale` | `Float` | O | 코 크기 배율 |
| `mouthSizeScale` | `Float` | O | 입 크기 배율 |

예시:

```json
{
  "headSizeScale": 1.0,
  "bodySizeScale": 1.0,
  "eyeSizeScale": 1.0,
  "noseSizeScale": 1.0,
  "mouthSizeScale": 1.0
}
```

### PlazaChatMessageResponse

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `id` | `String` | O | 메시지 ID |
| `senderUserId` | `String` | O | 보낸 사용자 ID |
| `senderNickname` | `String` | O | 보낸 사용자 닉네임 |
| `text` | `String` | O | 메시지 내용 |
| `sentAtMillis` | `Long` | O | 메시지 전송 시각 |

`plazaId`는 URL path에 포함되어 있으므로 MVP 응답 필드에서는 제외 가능하다.

예시:

```json
{
  "id": "plaza_message_001",
  "senderUserId": "user_me",
  "senderNickname": "나의루파",
  "text": "안녕!",
  "sentAtMillis": 1778509810000
}
```

### PlazaPositionResponse

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `x` | `Float` | O | 광장 내 x 위치 |
| `y` | `Float` | O | 광장 내 y 위치 |

MVP에서는 서버가 단순 고정 위치 또는 랜덤 위치만 내려주면 된다.

프론트는 현재 정규화 좌표 기반 렌더링을 사용하고 있으므로 `0.0 ~ 1.0` 범위의 `Float` 값을 권장한다.

예시:

```json
{
  "x": 0.42,
  "y": 0.68
}
```

## MVP 에러 응답

에러 응답은 모든 광장 API에서 동일한 구조를 사용한다.

```json
{
  "code": "PLAZA_FULL",
  "message": "광장이 가득 찼어요."
}
```

| code | HTTP status | 설명 |
|---|---:|---|
| `PLAZA_NOT_FOUND` | 404 | 광장을 찾을 수 없음 |
| `PLAZA_FULL` | 409 | 광장 정원이 가득 참 |
| `INVALID_PLAZA_CODE` | 400 | 광장 코드 형식이 올바르지 않음 |
| `NOT_IN_PLAZA` | 403 | 해당 광장 참가자가 아님 |
| `EMPTY_MESSAGE` | 400 | 메시지가 비어 있음 |
| `MESSAGE_TOO_LONG` | 400 | 메시지가 최대 길이를 초과함 |
| `UNAUTHORIZED` | 401 | 인증 토큰 없음 또는 인증 실패 |

## 후순위 확장 시 추가할 항목

MVP 완료 후 광장 경험을 확장할 때 아래 항목을 추가한다.

### 펫 상호작용

- `POST /plazas/{plazaId}/interactions`
- `PlazaInteractionEvent`
- 상호작용 타입: `GREET`, `PLAY`, `REST`, `FOLLOW`
- 상호작용 전용 에러 코드

### 실시간 동기화

- `WebSocket /ws/plazas/{plazaId}`
- `PLAZA_SNAPSHOT` broadcast
- 입장, 퇴장, 채팅, 상호작용 이벤트 실시간 전파
- WebSocket 연결 실패/재연결 처리

### 이동 및 서버 보정

- `PlazaMovementCommand`
- `movement`
- `positionUpdatedAtMillis`
- `serverTime`
- `isServerAuthoritative`
- 서버 tick / scheduled job
- 서버 시간 기반 위치 보간

## 백엔드 구현 체크리스트

- [ ] 기존 `POST /plazas/random/join` 응답을 MVP `PlazaRoomResponse` 기준으로 정리한다.
- [ ] 기존 `POST /plazas/code/join`의 코드 검증 및 실패 응답을 MVP 에러 코드 기준으로 정리한다.
- [ ] 기존 `GET /plazas/me/active`에서 입장 중인 광장이 없으면 `{ "plaza": null }`을 반환한다.
- [ ] `GET /plazas/{plazaId}`를 추가한다.
- [ ] `POST /plazas/{plazaId}/leave`를 추가한다.
- [ ] `POST /plazas/{plazaId}/messages`를 추가한다.
- [ ] 광장 참가자 목록을 `PlazaParticipantResponse[]`로 응답한다.
- [ ] 참가자별 펫 정보를 `PlazaPetSnapshotResponse`로 응답한다.
- [ ] 최근 채팅 메시지를 `PlazaChatMessageResponse[]`로 응답한다.
- [ ] 광장 예외가 `500 UNKNOWN`으로 내려가지 않도록 `code/message` 에러 응답을 정리한다.
- [ ] 참가자 수가 `maxParticipants`를 초과하지 않도록 검증한다.
- [ ] 스냅샷 조회, 퇴장, 채팅 전송 시 현재 사용자의 광장 참가 여부를 검증한다.
- [ ] MVP DTO에서 `interactions`, `movement`, `serverTime`, `isServerAuthoritative` 필드를 제거한다.
