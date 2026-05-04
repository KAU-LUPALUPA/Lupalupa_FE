# 루파루파 친구 API 계약서

버전: `0.1`  
상태: MVP 백엔드 연동용 초안  
범위: 친구 코드, 친구 신청/수락/거절/취소, 친구 목록, 친구 집 방문, 친구 메시지

## 공통 규칙

| 항목 | 내용 |
|---|---|
| Base URL | 미정. 예시: `https://api.lupalupa.com` |
| 인증 | 모든 API는 `Authorization: Bearer {accessToken}` 필요 |
| Content-Type | `application/json` |
| 시간 형식 | ISO-8601 문자열. 예: `2026-05-04T18:30:00+09:00` |
| 친구 코드 형식 | 서버 저장값은 `LUPA5B0RI`, 화면 표시값은 `LUPA-5B0RI` 권장 |
| 공통 에러 Response | `{ "code": "ERROR_CODE", "message": "사용자 표시 메시지" }` |

## API 목록

| 기능 | 카테고리 | 사용자 | Method | URL | param | 설명 | Response | 비고 |
|---|---|---|---|---|---|---|---|---|
| 내 친구 코드 조회 | 친구 코드 | 로그인 사용자 | GET | `/users/me/friend-code` | 없음 | 친구 화면 진입 시 내 친구 코드를 조회한다. | `friendCode`, `displayFriendCode` | 닉네임/프로필은 로그인 응답 또는 `/users/me` 응답을 사용한다. |
| 친구 코드로 유저 조회 | 친구 코드 | 로그인 사용자 | GET | `/friends/users/by-code` | Query: `friendCode` | 친구 신청 전 상대 유저가 존재하는지 확인한다. | `user{userId, nickname, friendCode, displayFriendCode, avatarAssetKey}`, `relationshipStatus` | 선택 API. MVP에서 바로 신청만 할 경우 생략 가능. |
| 친구 신청 보내기 | 친구 요청 | 로그인 사용자 | POST | `/friends/requests` | Body: `friendCode` | 상대방 친구 코드를 입력해 친구 신청을 보낸다. | `request{id, fromUser, toUser, status, createdAt, respondedAt}` | 에러: `EMPTY_CODE`, `SELF_CODE`, `USER_NOT_FOUND`, `ALREADY_FRIENDS`, `REQUEST_ALREADY_SENT`, `REQUEST_ALREADY_RECEIVED`, `BLOCKED` |
| 받은 친구 신청 목록 | 친구 요청 | 로그인 사용자 | GET | `/friends/requests/received` | 없음 | 나에게 온 친구 신청 목록을 조회한다. | `requests[]` | 친구요청목록 탭에서 사용한다. |
| 보낸 친구 신청 목록 | 친구 요청 | 로그인 사용자 | GET | `/friends/requests/sent` | 없음 | 내가 보낸 친구 신청 목록을 조회한다. | `requests[]` | 보낸 요청 탭에서 사용한다. |
| 친구 신청 수락 | 친구 요청 | 요청 수신자 | POST | `/friends/requests/{requestId}/accept` | Path: `requestId` | 받은 친구 신청을 수락하고 양방향 친구 관계를 생성한다. | `request{id, status, respondedAt}`, `friendship{friendshipId, friend, status, friendsSince}` | 에러: `REQUEST_NOT_FOUND`, `REQUEST_NOT_PENDING`, `BLOCKED` |
| 친구 신청 거절 | 친구 요청 | 요청 수신자 | POST | `/friends/requests/{requestId}/reject` | Path: `requestId` | 받은 친구 신청을 거절한다. | `request{id, status, respondedAt}` | 거절 후 재신청 허용 여부는 서버 정책으로 결정. MVP는 재신청 허용 권장. |
| 보낸 친구 신청 취소 | 친구 요청 | 요청 발신자 | POST | `/friends/requests/{requestId}/cancel` | Path: `requestId` | 내가 보낸 친구 신청을 취소한다. | `request{id, status, respondedAt}` | 에러: `REQUEST_NOT_FOUND`, `REQUEST_NOT_PENDING` |
| 친구 목록 조회 | 친구 목록 | 로그인 사용자 | GET | `/friends` | 없음 | 수락된 친구 목록을 조회한다. | `friends[]` | 친구 탭 기본 데이터. |
| 친구 삭제 | 친구 목록 | 로그인 사용자 | DELETE | `/friends/{friendUserId}` | Path: `friendUserId` | 친구 관계를 삭제한다. | `204 No Content` | 에러: `FRIEND_NOT_FOUND`, `NOT_FRIENDS` |
| 친구 집 방문 정보 조회 | 친구 집 | 친구 관계 사용자 | GET | `/friends/{friendUserId}/home` | Path: `friendUserId` | 친구의 집 화면을 열 때 필요한 유저, 방, 펫 정보를 조회한다. | `owner`, `room`, `visitedAt` | 친구가 아닌 유저는 `NOT_FRIENDS`. 방 좌표계는 내 방 API와 동일하게 사용. |
| 친구 메시지 목록 조회 | 친구 메시지 | 친구 관계 사용자 | GET | `/friends/{friendUserId}/messages` | Path: `friendUserId`<br>Query: `limit`, `before` | 친구와 주고받은 짧은 메시지 목록을 조회한다. | `messages[]`, `nextCursor` | MVP에서는 REST 폴링 방식 가능. 기본 `limit=30` 권장. |
| 친구 메시지 보내기 | 친구 메시지 | 친구 관계 사용자 | POST | `/friends/{friendUserId}/messages` | Path: `friendUserId`<br>Body: `text` | 친구에게 짧은 메시지를 보낸다. | `message{id, friendUserId, sender, text, sentAt}` | `text` 최대 120자. 에러: `EMPTY_MESSAGE`, `MESSAGE_TOO_LONG`, `NOT_FRIENDS` |

## Request / Response 예시

### 친구 신청 보내기

```http
POST /friends/requests
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "friendCode": "LUPA5B0RI"
}
```

```json
{
  "request": {
    "id": "friend_request_001",
    "fromUser": {
      "userId": "user_me",
      "nickname": "나의루파",
      "friendCode": "LUPAME01",
      "displayFriendCode": "LUPA-ME01",
      "avatarAssetKey": null
    },
    "toUser": {
      "userId": "user_friend",
      "nickname": "친구루파",
      "friendCode": "LUPA5B0RI",
      "displayFriendCode": "LUPA-5B0RI",
      "avatarAssetKey": null
    },
    "status": "PENDING",
    "createdAt": "2026-05-04T18:30:00+09:00",
    "respondedAt": null
  }
}
```

### 친구 목록 조회

```http
GET /friends
Authorization: Bearer {accessToken}
```

```json
{
  "friends": [
    {
      "friendshipId": "friendship_001",
      "user": {
        "userId": "user_friend",
        "nickname": "친구루파",
        "friendCode": "LUPA5B0RI",
        "displayFriendCode": "LUPA-5B0RI",
        "avatarAssetKey": null
      },
      "status": "ACCEPTED",
      "friendsSince": "2026-05-04T18:30:00+09:00"
    }
  ]
}
```

### 친구 집 방문 정보 조회

```http
GET /friends/{friendUserId}/home
Authorization: Bearer {accessToken}
```

```json
{
  "owner": {
    "userId": "user_friend",
    "nickname": "친구루파",
    "friendCode": "LUPA5B0RI",
    "displayFriendCode": "LUPA-5B0RI",
    "avatarAssetKey": null
  },
  "room": {
    "sceneId": "main_room",
    "wallAssetKey": "room/walls/main_wall",
    "floorAssetKey": "room/floors/main_floor",
    "placedItems": [
      {
        "placedItemId": "placed_bed_001",
        "itemId": "bed_basic",
        "objectType": "BED",
        "anchorType": "FLOOR",
        "anchor": {
          "u": 0.25,
          "v": 0.25
        },
        "tile": {
          "x": 0,
          "y": 0,
          "widthTiles": 2,
          "depthTiles": 2,
          "anchorMode": "CENTER"
        }
      }
    ],
    "layoutRevision": 12,
    "updatedAt": "2026-05-04T18:30:00+09:00"
  },
  "pet": {
    "petId": "pet_friend",
    "ownerUserId": "user_friend",
    "name": "루파",
    "characterAssetKey": "room/characters/lupa_default",
    "appearance": {
      "headSizeScale": 1.08,
      "bodySizeScale": 0.96,
      "eyeSizeScale": 1.12,
      "noseSizeScale": 0.92,
      "mouthSizeScale": 1.04
    },
    "status": {
      "hunger": 80,
      "fatigue": 25,
      "isEgg": false
    },
    "personality": "ACTIVE",
    "equippedItemIds": [],
    "anchor": {
      "u": 0.44,
      "v": 0.64
    }
  },
  "visitedAt": "2026-05-04T18:30:00+09:00"
}
```

### 친구 메시지 보내기

```http
POST /friends/{friendUserId}/messages
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "text": "안녕! 놀러왔어"
}
```

```json
{
  "message": {
    "id": "friend_message_001",
    "friendUserId": "user_friend",
    "sender": "ME",
    "text": "안녕! 놀러왔어",
    "sentAt": "2026-05-04T18:30:00+09:00"
  }
}
```

## 데이터 모델

### FriendUser

| 필드 | 타입 | 설명 |
|---|---|---|
| `userId` | `String` | 서버 내부 유저 식별자 |
| `nickname` | `String` | 화면 표시 이름 |
| `friendCode` | `String` | 친구 추가 요청에 사용하는 코드 |
| `displayFriendCode` | `String` | 화면 표시용 친구 코드 |
| `avatarAssetKey` | `String?` | 프로필 이미지 에셋 키. 없으면 `null` |

### FriendRequest

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `String` | 친구 요청 ID |
| `fromUser` | `FriendUser` | 요청 보낸 유저 |
| `toUser` | `FriendUser` | 요청 받은 유저 |
| `status` | `FriendRequestStatus` | `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELED` |
| `createdAt` | `String` | 요청 생성 시각 |
| `respondedAt` | `String?` | 수락/거절/취소 시각 |

### FriendSummary

| 필드 | 타입 | 설명 |
|---|---|---|
| `friendshipId` | `String` | 친구 관계 ID |
| `user` | `FriendUser` | 친구 유저 정보 |
| `status` | `FriendshipStatus` | 기본값 `ACCEPTED` |
| `friendsSince` | `String` | 친구가 된 시각 |

### FriendMessage

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `String` | 메시지 ID |
| `friendUserId` | `String` | 대화 상대 유저 ID |
| `sender` | `FriendMessageSender` | `ME`, `FRIEND` |
| `text` | `String` | 메시지 내용. 최대 120자 |
| `sentAt` | `String` | 발송 시각 |

## 상태값

| 구분 | 값 | 설명 |
|---|---|---|
| 친구 요청 상태 | `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELED` | 친구 신청 처리 상태 |
| 친구 관계 상태 | `NONE`, `PENDING_SENT`, `PENDING_RECEIVED`, `ACCEPTED`, `REJECTED`, `CANCELED`, `BLOCKED` | 두 유저 간 관계 상태 |
| 메시지 발신자 | `ME`, `FRIEND` | 내 메시지인지 친구 메시지인지 구분 |

## 에러 코드

| code | HTTP | 설명 |
|---|---:|---|
| `EMPTY_CODE` | `400` | 친구 코드가 비어 있음 |
| `SELF_CODE` | `400` | 내 친구 코드를 입력함 |
| `USER_NOT_FOUND` | `404` | 친구 코드에 해당하는 유저가 없음 |
| `ALREADY_FRIENDS` | `409` | 이미 친구인 유저 |
| `REQUEST_ALREADY_SENT` | `409` | 이미 내가 보낸 친구 요청이 있음 |
| `REQUEST_ALREADY_RECEIVED` | `409` | 이미 상대에게서 받은 요청이 있음 |
| `REQUEST_NOT_FOUND` | `404` | 친구 요청을 찾을 수 없음 |
| `REQUEST_NOT_PENDING` | `409` | 이미 처리된 친구 요청 |
| `FRIEND_NOT_FOUND` | `404` | 친구 정보를 찾을 수 없음 |
| `NOT_FRIENDS` | `403` | 친구 관계가 아니라 접근할 수 없음 |
| `FRIEND_HOME_UNAVAILABLE` | `503` | 친구 집 정보를 불러올 수 없음 |
| `EMPTY_MESSAGE` | `400` | 메시지가 비어 있음 |
| `MESSAGE_TOO_LONG` | `400` | 메시지가 최대 길이를 초과함 |
| `BLOCKED` | `403` | 차단 또는 정책상 요청 불가 |

## 백엔드와 합의할 사항

| 항목 | 제안 |
|---|---|
| 친구 코드 생성 | 서버에서 최초 회원 생성 시 자동 생성 |
| 친구 코드 정규화 | 입력값의 공백, 하이픈, 대소문자 차이는 서버에서 정규화 |
| 친구 요청 중복 정책 | 두 유저 사이에 `PENDING` 요청은 한 개만 유지 |
| 거절 후 재신청 | MVP에서는 허용 권장 |
| 친구 삭제 후 재신청 | MVP에서는 허용 권장 |
| 친구 집 접근 | 친구 관계가 아니면 `403 NOT_FRIENDS` |
| 메시지 방식 | MVP는 REST 조회/전송, 추후 WebSocket 확장 가능 |
| 메시지 최대 길이 | 현재 앱 코드 기준 120자 |
