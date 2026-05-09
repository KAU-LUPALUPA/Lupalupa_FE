# 루파루파 API 계약서 초안

버전: `0.1`  
상태: MVP 백엔드 연동용 초안  
최종 수정일: 2026-05-09

## 1. 문서 목적

이 문서는 현재 Android 앱에서 MVP 백엔드 연동을 위해 필요한 API 계약을 정리한다.

포함 범위는 다음과 같다.

- 카카오 로그인 및 앱 자체 토큰 발급
- 현재 로그인한 유저 정보
- 개인 친구 코드
- 펫 외형, 상태, 성격, 치장 정보
- 친구 신청, 수락, 거절, 취소
- 친구 목록
- 친구 집 초대 기반 방문
- 친구 간 간단한 메시지
- 내 방 레이아웃 검증 및 저장
- 재화, 상점, 인벤토리
- 미니게임 보상
- 스크린샷 갤러리 메타데이터 동기화

## 2. 공통 규칙

### 2.1 Base URL

Base URL은 백엔드 배포 환경에 맞춰 추후 확정한다.

```http
https://api.lupalupa.com
```

로컬 개발 환경 예시는 다음과 같다.

```http
http://localhost:8080
```

### 2.2 인증 방식

로그인이 필요한 API는 Bearer Token을 사용한다.

```http
Authorization: Bearer {accessToken}
Content-Type: application/json
```

### 2.3 시간 형식

서버 응답의 시간 값은 ISO-8601 문자열을 사용한다.

```json
"2026-05-04T18:30:00+09:00"
```

Android 앱의 기존 mock 데이터에는 millisecond timestamp가 일부 사용될 수 있지만, 서버 계약에서는 타임존 혼동을 줄이기 위해 ISO-8601 문자열을 기준으로 한다.

### 2.4 응답 형태

성공 응답은 JSON object를 반환한다.

목록 응답은 root array를 직접 반환하지 않고, 명확한 필드명으로 감싼다.

좋은 예:

```json
{
  "friends": []
}
```

피해야 할 예:

```json
[]
```

### 2.5 공통 에러 응답

에러 응답은 다음 형태를 사용한다.

```json
{
  "code": "FRIEND_REQUEST_ALREADY_SENT",
  "message": "이미 친구 요청을 보냈습니다."
}
```

`code`는 클라이언트가 분기 처리할 수 있는 고정 문자열이다.  
`message`는 사용자에게 보여줄 수 있는 한국어 메시지다.

### 2.6 권장 HTTP 상태 코드

| Status | 의미 |
|---:|---|
| `200` | 조회 또는 일반 액션 성공 |
| `201` | 리소스 생성 성공 |
| `204` | 삭제 또는 응답 본문 없는 액션 성공 |
| `400` | 요청 값 오류 |
| `401` | 인증 토큰 없음 또는 만료 |
| `403` | 권한 없음 |
| `404` | 대상 리소스 없음 |
| `409` | 중복 요청, 이미 친구, 이미 구매 등 충돌 |
| `500` | 서버 내부 오류 |

## 3. 인증 API

### 3.1 카카오 로그인

카카오 SDK에서 받은 Kakao access token을 서버로 전달하면, 서버는 앱 자체 access token과 refresh token을 발급한다.

```http
POST /auth/kakao
```

Request:

```json
{
  "kakaoAccessToken": "kakao-access-token"
}
```

Response:

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "user": {
    "userId": "user_123",
    "nickname": "루파유저",
    "friendCode": "LUPA4821",
    "avatarAssetKey": null
  }
}
```

비고:

- `userId`는 서버에서 부여하는 내부 유저 식별자다.
- `friendCode`는 친구 추가에 사용하는 공개 코드다.
- `friendCode`는 서버에서 생성한다.
- 최초 로그인 유저라면 서버에서 유저 row를 생성한다.
- 기존 유저라면 기존 정보를 반환한다.

### 3.2 토큰 갱신

```http
POST /auth/refresh
```

Request:

```json
{
  "refreshToken": "jwt-refresh-token"
}
```

Response:

```json
{
  "accessToken": "new-jwt-access-token",
  "refreshToken": "new-jwt-refresh-token"
}
```

가능한 에러:

```text
INVALID_REFRESH_TOKEN
EXPIRED_REFRESH_TOKEN
```

## 4. 유저 API

### 4.1 내 정보 조회

```http
GET /users/me
```

Response:

```json
{
  "userId": "user_123",
  "nickname": "루파유저",
  "friendCode": "LUPA4821",
  "avatarAssetKey": null,
  "currencyAmount": 100
}
```

### 4.2 내 친구 코드 조회

```http
GET /users/me/friend-code
```

Response:

```json
{
  "friendCode": "LUPA4821",
  "displayFriendCode": "LUPA-4821"
}
```

비고:

- `userId`는 서버 내부에서 사용하는 고유 식별자다.
- 친구 추가 화면에는 `userId`를 직접 노출하지 않고 `friendCode`를 사용한다.
- `friendCode`는 실제 API 요청에 사용하는 값이다.
- `displayFriendCode`는 화면 표시용이다.
- 앱에서 입력 시 하이픈은 제거해서 보내는 방식도 허용할 수 있다.

### 4.3 내 펫 정보 조회

현재 로그인한 유저의 펫 외형, 상태, 성격, 치장 정보를 조회한다.

```http
GET /pets/me
```

Response:

```json
{
  "pet": {
    "petId": "pet_123",
    "ownerUserId": "user_123",
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
      "satiety": 80,
      "vitality": 75,
      "isEgg": false,
      "action": "IDLE",
      "anchor": {
        "u": 0.44,
        "v": 0.64
      }
    },
    "personality": "ACTIVE",
    "equippedItemIds": [
      "item_red_hat",
      "item_blue_shoes"
    ],
    "createdAt": "2026-05-04T18:30:00+09:00",
    "updatedAt": "2026-05-04T22:20:00+09:00"
  }
}
```

펫 외형 필드:

| 필드 | 설명 |
|---|---|
| `headSizeScale` | 머리 크기 배율 |
| `bodySizeScale` | 몸 크기 배율 |
| `eyeSizeScale` | 눈 크기 배율 |
| `noseSizeScale` | 코 크기 배율 |
| `mouthSizeScale` | 입 크기 배율 |

비고:

- 외형 크기 값은 최초 펫 생성 시 서버에서 랜덤으로 생성한다.
- 권장 범위는 `0.85`부터 `1.15`까지다.
- 한 번 생성된 외형 값은 기본적으로 유지한다.
- 외형 재설정 기능을 만들 경우 별도 API로 분리한다.

펫 상태 필드:

| 필드 | 설명 |
|---|---|
| `satiety` | 포만감 수치. `0`부터 `100`까지. 높을수록 배부른 상태 |
| `vitality` | 활력 수치. `0`부터 `100`까지. 높을수록 활기찬 상태 |
| `isEgg` | 알 상태 여부 |
| `action` | 현재 행동 상태 |
| `anchor` | 방 바닥 위 현재 위치 |

`action` 값:

```text
IDLE | RESTING | PLAYING | EATING
```

`personality` 값:

```text
ACTIVE | CALM | LAZY
```

화면 표시 라벨:

| 값 | 표시 |
|---|---|
| `ACTIVE` | 활발 |
| `CALM` | 차분 |
| `LAZY` | 게으름 |

치장 아이템:

- 착용한 치장 아이템이 없으면 `equippedItemIds`는 빈 배열 `[]`로 내려준다.
- 각 값은 유저가 보유한 치장 아이템의 item id 또는 inventory item id를 사용한다.
- MVP에서는 부위별 슬롯 구조 없이 id 배열로 시작한다.
- 추후 모자, 신발, 악세서리처럼 부위 충돌 검증이 필요해지면 슬롯 구조로 확장한다.

### 4.4 내 펫 상태 업데이트

먹이, 휴식, 놀이, 알 부화 등으로 펫 상태가 바뀔 때 사용한다.

```http
PATCH /pets/me/status
```

Request:

```json
{
  "satiety": 75,
  "vitality": 70,
  "isEgg": false,
  "action": "PLAYING",
  "anchor": {
    "u": 0.52,
    "v": 0.68
  }
}
```

Response:

```json
{
  "pet": {
    "petId": "pet_123",
    "ownerUserId": "user_123",
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
      "satiety": 75,
      "vitality": 70,
      "isEgg": false,
      "action": "PLAYING",
      "anchor": {
        "u": 0.52,
        "v": 0.68
      }
    },
    "personality": "ACTIVE",
    "equippedItemIds": [
      "item_red_hat",
      "item_blue_shoes"
    ],
    "updatedAt": "2026-05-04T22:25:00+09:00"
  }
}
```

가능한 에러:

```text
PET_NOT_FOUND
INVALID_SATIETY
INVALID_VITALITY
INVALID_ACTION
INVALID_ANCHOR
```

비고:

- `satiety`와 `vitality`는 서버에서 `0`부터 `100` 사이로 검증한다.
- 운영 단계에서는 클라이언트가 임의로 수치를 보내는 방식보다 서버가 행동 결과에 따라 수치를 계산하는 방식이 더 안전하다.
- MVP에서는 클라이언트 요청 값을 서버가 검증 후 반영하는 방식으로 시작할 수 있다.

### 4.5 내 펫 치장 아이템 변경

펫이 착용 중인 치장 아이템 목록을 변경한다.

```http
PUT /pets/me/equipment
```

Request:

```json
{
  "equippedItemIds": [
    "item_red_hat",
    "item_blue_shoes"
  ]
}
```

착용 아이템이 없으면 빈 배열로 보낸다.

```json
{
  "equippedItemIds": []
}
```

Response:

```json
{
  "equippedItemIds": [
    "item_red_hat",
    "item_blue_shoes"
  ],
  "updatedAt": "2026-05-04T22:30:00+09:00"
}
```

가능한 에러:

```text
PET_NOT_FOUND
ITEM_NOT_OWNED
ITEM_NOT_EQUIPPABLE
EQUIPMENT_SLOT_CONFLICT
```

## 5. 친구 API

### 5.1 공통 모델

친구 유저 모델:

```json
{
  "userId": "user_456",
  "nickname": "친구루파",
  "friendCode": "LUPA9912",
  "displayFriendCode": "LUPA-9912",
  "avatarAssetKey": null
}
```

친구 요청 상태:

```text
PENDING | ACCEPTED | REJECTED | CANCELED
```

두 유저 간 관계 상태:

```text
NONE | PENDING_SENT | PENDING_RECEIVED | ACCEPTED | REJECTED | CANCELED | BLOCKED
```

친구 집 초대 상태:

```text
PENDING | ACCEPTED | REJECTED | CANCELED | EXPIRED
```

### 5.2 친구 신청 보내기

상대방의 친구 코드를 입력해서 친구 신청을 보낸다.

```http
POST /friends/requests
```

Request:

```json
{
  "friendCode": "LUPA4821"
}
```

Response:

```json
{
  "request": {
    "id": "friend_request_001",
    "fromUser": {
      "userId": "user_123",
      "nickname": "나",
      "friendCode": "LUPA1234",
      "displayFriendCode": "LUPA-1234",
      "avatarAssetKey": null
    },
    "toUser": {
      "userId": "user_456",
      "nickname": "친구루파",
      "friendCode": "LUPA4821",
      "displayFriendCode": "LUPA-4821",
      "avatarAssetKey": null
    },
    "status": "PENDING",
    "createdAt": "2026-05-04T18:30:00+09:00",
    "respondedAt": null
  }
}
```

가능한 에러:

```text
EMPTY_CODE
SELF_CODE
USER_NOT_FOUND
ALREADY_FRIENDS
REQUEST_ALREADY_SENT
REQUEST_ALREADY_RECEIVED
BLOCKED
```

### 5.3 받은 친구 신청 목록

```http
GET /friends/requests/received
```

Response:

```json
{
  "requests": [
    {
      "id": "friend_request_001",
      "fromUser": {
        "userId": "user_456",
        "nickname": "친구루파",
        "friendCode": "LUPA4821",
        "displayFriendCode": "LUPA-4821",
        "avatarAssetKey": null
      },
      "toUser": {
        "userId": "user_123",
        "nickname": "나",
        "friendCode": "LUPA1234",
        "displayFriendCode": "LUPA-1234",
        "avatarAssetKey": null
      },
      "status": "PENDING",
      "createdAt": "2026-05-04T18:30:00+09:00",
      "respondedAt": null
    }
  ]
}
```

### 5.4 보낸 친구 신청 목록

```http
GET /friends/requests/sent
```

Response:

```json
{
  "requests": [
    {
      "id": "friend_request_002",
      "fromUser": {
        "userId": "user_123",
        "nickname": "나",
        "friendCode": "LUPA1234",
        "displayFriendCode": "LUPA-1234",
        "avatarAssetKey": null
      },
      "toUser": {
        "userId": "user_456",
        "nickname": "친구루파",
        "friendCode": "LUPA4821",
        "displayFriendCode": "LUPA-4821",
        "avatarAssetKey": null
      },
      "status": "PENDING",
      "createdAt": "2026-05-04T18:30:00+09:00",
      "respondedAt": null
    }
  ]
}
```

### 5.5 친구 신청 수락

```http
POST /friends/requests/{requestId}/accept
```

Response:

```json
{
  "request": {
    "id": "friend_request_001",
    "fromUser": {
      "userId": "user_456",
      "nickname": "친구루파",
      "friendCode": "LUPA4821",
      "displayFriendCode": "LUPA-4821",
      "avatarAssetKey": null
    },
    "toUser": {
      "userId": "user_123",
      "nickname": "나",
      "friendCode": "LUPA1234",
      "displayFriendCode": "LUPA-1234",
      "avatarAssetKey": null
    },
    "status": "ACCEPTED",
    "createdAt": "2026-05-04T18:30:00+09:00",
    "respondedAt": "2026-05-04T18:35:00+09:00"
  },
  "friendship": {
    "friendshipId": "friendship_001",
    "friend": {
      "userId": "user_456",
      "nickname": "친구루파",
      "friendCode": "LUPA4821",
      "displayFriendCode": "LUPA-4821",
      "avatarAssetKey": null
    },
    "status": "ACCEPTED",
    "friendsSince": "2026-05-04T18:35:00+09:00"
  }
}
```

가능한 에러:

```text
REQUEST_NOT_FOUND
REQUEST_NOT_PENDING
BLOCKED
```

비고:

- `request`는 `id`, `fromUser`, `toUser`, `status`, `createdAt`, `respondedAt`을 모두 포함하는 full `FriendRequest` 형태로 반환한다.
- 이미 처리된 요청은 `REQUEST_NOT_PENDING`으로 통일한다.

### 5.6 친구 신청 거절

```http
POST /friends/requests/{requestId}/reject
```

Response:

```json
{
  "request": {
    "id": "friend_request_001",
    "fromUser": {
      "userId": "user_456",
      "nickname": "친구루파",
      "friendCode": "LUPA4821",
      "displayFriendCode": "LUPA-4821",
      "avatarAssetKey": null
    },
    "toUser": {
      "userId": "user_123",
      "nickname": "나",
      "friendCode": "LUPA1234",
      "displayFriendCode": "LUPA-1234",
      "avatarAssetKey": null
    },
    "status": "REJECTED",
    "createdAt": "2026-05-04T18:30:00+09:00",
    "respondedAt": "2026-05-04T18:35:00+09:00"
  }
}
```

가능한 에러:

```text
REQUEST_NOT_FOUND
REQUEST_NOT_PENDING
BLOCKED
```

### 5.7 보낸 친구 신청 취소

```http
POST /friends/requests/{requestId}/cancel
```

Response:

```json
{
  "request": {
    "id": "friend_request_001",
    "fromUser": {
      "userId": "user_123",
      "nickname": "나",
      "friendCode": "LUPA1234",
      "displayFriendCode": "LUPA-1234",
      "avatarAssetKey": null
    },
    "toUser": {
      "userId": "user_456",
      "nickname": "친구루파",
      "friendCode": "LUPA4821",
      "displayFriendCode": "LUPA-4821",
      "avatarAssetKey": null
    },
    "status": "CANCELED",
    "createdAt": "2026-05-04T18:30:00+09:00",
    "respondedAt": "2026-05-04T18:35:00+09:00"
  }
}
```

가능한 에러:

```text
REQUEST_NOT_FOUND
REQUEST_NOT_PENDING
BLOCKED
```

### 5.8 친구 목록 조회

```http
GET /friends
```

Response:

```json
{
  "friends": [
    {
      "friendshipId": "friendship_001",
      "user": {
        "userId": "user_456",
        "nickname": "친구루파",
        "friendCode": "LUPA4821",
        "displayFriendCode": "LUPA-4821",
        "avatarAssetKey": null
      },
      "status": "ACCEPTED",
      "friendsSince": "2026-05-04T18:35:00+09:00"
    }
  ]
}
```

### 5.9 친구 삭제

```http
DELETE /friends/{friendUserId}
```

Response:

```http
204 No Content
```

가능한 에러:

```text
FRIEND_NOT_FOUND
NOT_FRIENDS
```

### 5.10 친구 집 초대 보내기

친구에게 내 집 방문 초대를 보낸다.

```http
POST /friends/home-invitations
```

Request:

```json
{
  "friendUserId": "user_456",
  "message": "우리 집 구경하러 올래?"
}
```

Response:

```json
{
  "invitation": {
    "id": "home_invitation_001",
    "fromUser": {
      "userId": "user_123",
      "nickname": "나",
      "friendCode": "LUPA1234",
      "displayFriendCode": "LUPA-1234",
      "avatarAssetKey": null
    },
    "toUser": {
      "userId": "user_456",
      "nickname": "친구루파",
      "friendCode": "LUPA4821",
      "displayFriendCode": "LUPA-4821",
      "avatarAssetKey": null
    },
    "status": "PENDING",
    "message": "우리 집 구경하러 올래?",
    "createdAt": "2026-05-09T18:30:00+09:00",
    "respondedAt": null,
    "expiresAt": "2026-05-09T19:30:00+09:00"
  }
}
```

가능한 에러:

```text
FRIEND_NOT_FOUND
NOT_FRIENDS
HOME_INVITATION_ALREADY_SENT
BLOCKED
```

비고:

- 초대 발신 UI의 진입점은 아직 확정하지 않는다.
- 현재 프론트 범위는 받은 초대를 우편함에서 확인하고 수락/거절하는 흐름이다.

### 5.11 받은 집 초대 목록

우편함에 표시할 내가 받은 대기 중인 집 초대 목록을 조회한다.

```http
GET /friends/home-invitations/received
```

Response:

```json
{
  "invitations": [
    {
      "id": "home_invitation_001",
      "fromUser": {
        "userId": "user_456",
        "nickname": "친구루파",
        "friendCode": "LUPA4821",
        "displayFriendCode": "LUPA-4821",
        "avatarAssetKey": null
      },
      "toUser": {
        "userId": "user_123",
        "nickname": "나",
        "friendCode": "LUPA1234",
        "displayFriendCode": "LUPA-1234",
        "avatarAssetKey": null
      },
      "status": "PENDING",
      "message": "우리 집 구경하러 올래?",
      "createdAt": "2026-05-09T18:30:00+09:00",
      "respondedAt": null,
      "expiresAt": "2026-05-09T19:30:00+09:00"
    }
  ]
}
```

### 5.12 보낸 집 초대 목록

내가 보낸 집 초대 목록을 조회한다. MVP에서는 선택 API다.

```http
GET /friends/home-invitations/sent
```

Response:

```json
{
  "invitations": []
}
```

### 5.13 집 초대 수락 및 방문

수신자가 우편함에서 `방문`을 누르면 호출한다.
성공 응답은 친구 집 화면을 그리는 데 필요한 읽기 전용 `homeSnapshot`을 반환한다.

```http
POST /friends/home-invitations/{invitationId}/accept
```

Response:

```json
{
  "invitation": {
    "id": "home_invitation_001",
    "fromUser": {
      "userId": "user_456",
      "nickname": "친구루파",
      "friendCode": "LUPA4821",
      "displayFriendCode": "LUPA-4821",
      "avatarAssetKey": null
    },
    "toUser": {
      "userId": "user_123",
      "nickname": "나",
      "friendCode": "LUPA1234",
      "displayFriendCode": "LUPA-1234",
      "avatarAssetKey": null
    },
    "status": "ACCEPTED",
    "message": "우리 집 구경하러 올래?",
    "createdAt": "2026-05-09T18:30:00+09:00",
    "respondedAt": "2026-05-09T18:35:00+09:00",
    "expiresAt": "2026-05-09T19:30:00+09:00"
  },
  "homeSnapshot": {
    "owner": {
      "userId": "user_456",
      "nickname": "친구루파",
      "friendCode": "LUPA4821",
      "displayFriendCode": "LUPA-4821",
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
      "updatedAt": "2026-05-09T18:30:00+09:00"
    },
    "petSnapshot": {
      "petId": "pet_456",
      "ownerUserId": "user_456",
      "name": "루파",
      "characterAssetKey": "room/characters/lupa_default",
      "appearance": {
        "headSizeScale": 1.08,
        "bodySizeScale": 0.96,
        "eyeSizeScale": 1.12,
        "noseSizeScale": 0.92,
        "mouthSizeScale": 1.04
      },
      "condition": {
        "satiety": 80,
        "vitality": 75,
        "isEgg": false
      },
      "sceneState": {
        "action": "IDLE",
        "anchor": {
          "u": 0.44,
          "v": 0.64
        }
      },
      "personality": "ACTIVE",
      "equippedItemIds": []
    },
    "snapshotAt": "2026-05-09T18:35:00+09:00",
    "visitedAt": "2026-05-09T18:35:00+09:00"
  }
}
```

가능한 에러:

```text
HOME_INVITATION_NOT_FOUND
HOME_INVITATION_NOT_PENDING
NOT_HOME_INVITATION_RECEIVER
NOT_FRIENDS
FRIEND_HOME_UNAVAILABLE
BLOCKED
```

### 5.14 집 초대 거절

```http
POST /friends/home-invitations/{invitationId}/reject
```

Response:

```json
{
  "invitation": {
    "id": "home_invitation_001",
    "fromUser": {
      "userId": "user_456",
      "nickname": "친구루파",
      "friendCode": "LUPA4821",
      "displayFriendCode": "LUPA-4821",
      "avatarAssetKey": null
    },
    "toUser": {
      "userId": "user_123",
      "nickname": "나",
      "friendCode": "LUPA1234",
      "displayFriendCode": "LUPA-1234",
      "avatarAssetKey": null
    },
    "status": "REJECTED",
    "message": "우리 집 구경하러 올래?",
    "createdAt": "2026-05-09T18:30:00+09:00",
    "respondedAt": "2026-05-09T18:34:00+09:00",
    "expiresAt": "2026-05-09T19:30:00+09:00"
  }
}
```

가능한 에러:

```text
HOME_INVITATION_NOT_FOUND
HOME_INVITATION_NOT_PENDING
NOT_HOME_INVITATION_RECEIVER
```

### 5.15 보낸 집 초대 취소

아직 수락되지 않은 보낸 초대를 취소한다. MVP에서는 선택 API다.

```http
POST /friends/home-invitations/{invitationId}/cancel
```

Response:

```json
{
  "invitation": {
    "id": "home_invitation_001",
    "fromUser": {
      "userId": "user_123",
      "nickname": "나",
      "friendCode": "LUPA1234",
      "displayFriendCode": "LUPA-1234",
      "avatarAssetKey": null
    },
    "toUser": {
      "userId": "user_456",
      "nickname": "친구루파",
      "friendCode": "LUPA4821",
      "displayFriendCode": "LUPA-4821",
      "avatarAssetKey": null
    },
    "status": "CANCELED",
    "message": "우리 집 구경하러 올래?",
    "createdAt": "2026-05-09T18:30:00+09:00",
    "respondedAt": "2026-05-09T18:34:00+09:00",
    "expiresAt": "2026-05-09T19:30:00+09:00"
  }
}
```

가능한 에러:

```text
HOME_INVITATION_NOT_FOUND
HOME_INVITATION_NOT_PENDING
NOT_HOME_INVITATION_SENDER
```

### 5.16 친구 집 초대 응답 구조 기준

| 구조 | 이유 |
|---|---|
| `homeSnapshot` | 친구 집 방문 화면에 필요한 데이터를 하나의 읽기 전용 묶음으로 다룬다. |
| `petSnapshot` | 방문 화면 렌더링용 펫 스냅샷임을 명확히 한다. |
| `condition` | `satiety`, `vitality`, `isEgg`처럼 펫 컨디션 값을 묶는다. |
| `sceneState` | `action`, `anchor`처럼 방문 화면의 위치와 행동 값을 묶는다. |

## 6. 친구 메시지 API

MVP에서는 실시간 채팅보다 “친구 방에 남기는 짧은 메시지”에 가깝게 시작하는 것을 권장한다.

### 6.1 친구와의 메시지 목록 조회

```http
GET /friends/{friendUserId}/messages
```

Query:

```http
?limit=30&before=message_123
```

Response:

```json
{
  "messages": [
    {
      "id": "message_001",
      "friendUserId": "user_456",
      "senderUserId": "user_123",
      "text": "집 예쁘다!",
      "sentAt": "2026-05-04T19:10:00+09:00"
    }
  ],
  "nextCursor": null
}
```

### 6.2 메시지 보내기

```http
POST /friends/{friendUserId}/messages
```

Request:

```json
{
  "text": "집 예쁘다!"
}
```

Response:

```json
{
  "message": {
    "id": "message_001",
    "friendUserId": "user_456",
    "senderUserId": "user_123",
    "text": "집 예쁘다!",
    "sentAt": "2026-05-04T19:10:00+09:00"
  }
}
```

가능한 에러:

```text
FRIEND_NOT_FOUND
NOT_FRIENDS
EMPTY_MESSAGE
MESSAGE_TOO_LONG
```

권장 제한:

- 메시지 최대 길이: 120자
- MVP에서는 이미지, 이모티콘, 실시간 socket은 제외
- 프론트는 `senderUserId == 현재 로그인 userId`인지 비교해 내 메시지 여부를 판단한다.

## 7. 내 방 레이아웃 동기화 API

앱 최초 접속 시 로컬에 저장된 방 레이아웃과 서버에 저장된 방 레이아웃을 비교한다.  
또한 사용자가 가구 배치, 벽, 바닥 타일을 변경할 때마다 서버에 최신 레이아웃을 저장한다.

이 API는 “방의 꾸미기 상태”만 다룬다.

- 현재 벽 이미지
- 현재 바닥 타일 이미지
- 배치된 가구 목록
- 각 가구의 위치, 크기, 회전, 배치 기준

펫의 현재 행동, 포만감, 활력, 알 상태, 치장 정보 같은 상태는 펫 API에서 별도로 관리한다.

### 7.1 방 레이아웃 모델

서버는 사용자의 방 레이아웃을 다음 형태로 저장한다.

```json
{
  "roomId": "room_user_123_main",
  "ownerUserId": "user_123",
  "layoutRevision": 12,
  "layoutHash": "sha256:abc123",
  "wallAssetKey": "room/walls/main_wall",
  "floorAssetKey": "room/floors/main_floor",
  "placedItems": [
    {
      "placementId": "placement_001",
      "inventoryItemId": "inventory_item_001",
      "shopItemId": "shop_item_001",
      "assetKey": "room/objects/bed_basic",
      "type": "BED",
      "anchorType": "FLOOR",
      "tilePlacement": {
        "tile": {
          "x": 0,
          "y": 0
        },
        "footprint": {
          "widthTiles": 2,
          "depthTiles": 2
        },
        "anchorMode": "FRONT_CENTER"
      },
      "wallPlacement": null,
      "scale": 1.0,
      "rotation": 0,
      "depthBias": 0
    }
  ],
  "updatedAt": "2026-05-04T21:30:00+09:00"
}
```

필드 설명:

| 필드 | 설명 |
|---|---|
| `layoutRevision` | 서버에서 증가시키는 레이아웃 버전 |
| `layoutHash` | 레이아웃 내용 기반 hash. 클라이언트 로컬 상태와 빠르게 비교할 때 사용 |
| `wallAssetKey` | 현재 방 벽 에셋 키 |
| `floorAssetKey` | 현재 방 바닥 타일 에셋 키 |
| `placedItems` | 현재 방에 배치된 가구 목록 |
| `placementId` | 배치 자체의 id. 같은 아이템을 다시 배치해도 추적 가능하게 하기 위한 값 |
| `inventoryItemId` | 유저가 보유한 인벤토리 아이템 id |
| `shopItemId` | 원본 상점 아이템 id |
| `assetKey` | 실제 렌더링에 사용할 에셋 키 |
| `anchorType` | `FLOOR` 또는 `WALL` |
| `tilePlacement` | 바닥 가구의 타일 기반 위치 |
| `wallPlacement` | 벽 장식의 벽면 기반 위치 |

`anchorType` 값:

```text
FLOOR | WALL
```

`anchorMode` 값:

```text
CENTER | FRONT_CENTER
```

`type` 값은 현재 Android 코드의 `RoomObjectType`과 맞춘다.

```text
BED | TOY_BOX | FOOD_BAG | WINDOW
```

비고:

- 현재 코드 기준 벽 변수명은 `wallAssetKey`다.
- 현재 코드 기준 바닥 변수명은 `floorAssetKey`다.
- 바닥 가구는 픽셀 좌표가 아니라 `tile.x`, `tile.y` 기반으로 저장하는 것을 권장한다.
- 화면 크기에 따라 실제 렌더링 위치는 Android에서 다시 계산한다.

### 7.2 앱 접속 시 로컬 레이아웃 검증

앱 최초 접속 또는 로그인 직후, 클라이언트가 로컬에 저장된 레이아웃 버전과 hash를 서버에 전달한다.  
서버는 로컬 상태가 최신인지 확인하고, 필요하면 서버의 최신 레이아웃을 내려준다.

```http
POST /rooms/me/layout/validate
```

Request:

```json
{
  "localLayoutRevision": 12,
  "localLayoutHash": "sha256:abc123",
  "localUpdatedAt": "2026-05-04T21:30:00+09:00"
}
```

로컬에 저장된 방 정보가 전혀 없는 최초 실행이라면 다음처럼 보낸다.

```json
{
  "localLayoutRevision": null,
  "localLayoutHash": null,
  "localUpdatedAt": null
}
```

Response - 로컬과 서버가 같은 경우:

```json
{
  "syncStatus": "MATCH",
  "serverLayoutRevision": 12,
  "serverLayoutHash": "sha256:abc123",
  "roomLayout": null
}
```

Response - 서버에 더 최신 레이아웃이 있는 경우:

```json
{
  "syncStatus": "SERVER_NEWER",
  "serverLayoutRevision": 13,
  "serverLayoutHash": "sha256:def456",
  "roomLayout": {
    "roomId": "room_user_123_main",
    "ownerUserId": "user_123",
    "layoutRevision": 13,
    "layoutHash": "sha256:def456",
    "wallAssetKey": "room/walls/main_wall",
    "floorAssetKey": "room/floors/main_floor",
    "placedItems": [],
    "updatedAt": "2026-05-04T22:00:00+09:00"
  }
}
```

`syncStatus` 값:

```text
MATCH | SERVER_NEWER | CLIENT_NEWER | CONFLICT
```

MVP 권장 처리:

- `MATCH`: 로컬 레이아웃을 그대로 사용한다.
- `SERVER_NEWER`: 서버 레이아웃을 로컬에 저장하고 화면에 적용한다.
- `CLIENT_NEWER`: MVP에서는 서버에 다시 저장을 시도하거나, 서버 우선 정책으로 덮어쓴다.
- `CONFLICT`: MVP에서는 서버 레이아웃을 우선 적용한다.

가능한 에러:

```text
ROOM_NOT_FOUND
INVALID_LAYOUT_REVISION
```

### 7.3 내 방 레이아웃 조회

검증 API와 별개로 서버의 현재 방 레이아웃을 직접 조회할 때 사용한다.

```http
GET /rooms/me/layout
```

Response:

```json
{
  "roomLayout": {
    "roomId": "room_user_123_main",
    "ownerUserId": "user_123",
    "layoutRevision": 13,
    "layoutHash": "sha256:def456",
    "wallAssetKey": "room/walls/main_wall",
    "floorAssetKey": "room/floors/main_floor",
    "placedItems": [
      {
        "placementId": "placement_001",
        "inventoryItemId": "inventory_item_001",
        "shopItemId": "shop_item_001",
        "assetKey": "room/objects/bed_basic",
        "type": "BED",
        "anchorType": "FLOOR",
        "tilePlacement": {
          "tile": {
            "x": 0,
            "y": 0
          },
          "footprint": {
            "widthTiles": 2,
            "depthTiles": 2
          },
          "anchorMode": "FRONT_CENTER"
        },
        "wallPlacement": null,
        "scale": 1.0,
        "rotation": 0,
        "depthBias": 0
      }
    ],
    "updatedAt": "2026-05-04T22:00:00+09:00"
  }
}
```

### 7.4 내 방 레이아웃 저장

사용자가 배치하기 또는 수정하기를 통해 벽, 바닥, 가구 배치를 바꾼 뒤 서버에 최신 레이아웃을 저장한다.

MVP에서는 부분 수정 API를 여러 개 두기보다, “현재 방 레이아웃 전체 snapshot”을 저장하는 방식을 권장한다.  
이렇게 하면 클라이언트와 서버가 같은 상태를 바라보는지 확인하기 쉽다.

```http
PUT /rooms/me/layout
```

Request:

```json
{
  "baseLayoutRevision": 13,
  "wallAssetKey": "room/walls/main_wall",
  "floorAssetKey": "room/floors/main_floor",
  "placedItems": [
    {
      "placementId": "placement_001",
      "inventoryItemId": "inventory_item_001",
      "shopItemId": "shop_item_001",
      "assetKey": "room/objects/bed_basic",
      "type": "BED",
      "anchorType": "FLOOR",
      "tilePlacement": {
        "tile": {
          "x": 1,
          "y": 2
        },
        "footprint": {
          "widthTiles": 2,
          "depthTiles": 2
        },
        "anchorMode": "FRONT_CENTER"
      },
      "wallPlacement": null,
      "scale": 1.0,
      "rotation": 0,
      "depthBias": 0
    }
  ]
}
```

Response:

```json
{
  "roomLayout": {
    "roomId": "room_user_123_main",
    "ownerUserId": "user_123",
    "layoutRevision": 14,
    "layoutHash": "sha256:ghi789",
    "wallAssetKey": "room/walls/main_wall",
    "floorAssetKey": "room/floors/main_floor",
    "placedItems": [
      {
        "placementId": "placement_001",
        "inventoryItemId": "inventory_item_001",
        "shopItemId": "shop_item_001",
        "assetKey": "room/objects/bed_basic",
        "type": "BED",
        "anchorType": "FLOOR",
        "tilePlacement": {
          "tile": {
            "x": 1,
            "y": 2
          },
          "footprint": {
            "widthTiles": 2,
            "depthTiles": 2
          },
          "anchorMode": "FRONT_CENTER"
        },
        "wallPlacement": null,
        "scale": 1.0,
        "rotation": 0,
        "depthBias": 0
      }
    ],
    "updatedAt": "2026-05-04T22:05:00+09:00"
  }
}
```

가능한 에러:

```text
ROOM_NOT_FOUND
LAYOUT_CONFLICT
ITEM_NOT_OWNED
INVALID_WALL_ASSET
INVALID_FLOOR_ASSET
INVALID_PLACEMENT
TILE_OUT_OF_BOUNDS
TILE_ALREADY_OCCUPIED
```

비고:

- `baseLayoutRevision`은 클라이언트가 마지막으로 알고 있던 서버 레이아웃 버전이다.
- 서버의 현재 `layoutRevision`과 `baseLayoutRevision`이 다르면 `409 LAYOUT_CONFLICT`를 반환한다.
- 충돌이 발생하면 클라이언트는 `GET /rooms/me/layout` 또는 `POST /rooms/me/layout/validate`로 최신 상태를 다시 받은 뒤 재시도한다.
- 서버는 `inventoryItemId`가 실제로 유저 소유인지 검증해야 한다.
- 서버는 같은 타일에 여러 가구가 겹치지 않는지 검증할 수 있다.
- 벽 장식은 `tilePlacement` 대신 `wallPlacement`를 사용한다.

벽 장식 배치 예시는 다음과 같다.

```json
{
  "placementId": "placement_window_001",
  "inventoryItemId": "inventory_window_001",
  "shopItemId": "shop_window_001",
  "assetKey": "room/decor/window_main",
  "type": "WINDOW",
  "anchorType": "WALL",
  "tilePlacement": null,
  "wallPlacement": {
    "face": "BACK",
    "u": 0.58,
    "v": 0.36
  },
  "scale": 1.0,
  "rotation": 0,
  "depthBias": 0
}
```

### 7.5 레이아웃 동기화 권장 흐름

앱 시작 시:

1. 카카오 로그인 또는 자동 로그인으로 앱 access token을 확보한다.
2. `GET /users/me`로 내 정보를 가져온다.
3. 로컬 DB 또는 DataStore에서 마지막 방 레이아웃의 `layoutRevision`, `layoutHash`를 읽는다.
4. `POST /rooms/me/layout/validate`를 호출한다.
5. `MATCH`면 로컬 레이아웃을 그대로 사용한다.
6. `SERVER_NEWER` 또는 `CONFLICT`면 서버가 내려준 `roomLayout`을 로컬에 저장하고 화면에 적용한다.

가구 배치 변경 시:

1. 사용자가 가구를 배치하거나 이동한다.
2. 클라이언트에서 임시로 화면 상태를 갱신한다.
3. `PUT /rooms/me/layout`로 전체 레이아웃 snapshot을 저장한다.
4. 성공하면 응답의 `layoutRevision`, `layoutHash`를 로컬에 저장한다.
5. 실패하면 서버 최신 레이아웃을 다시 가져와 화면 상태를 맞춘다.

## 8. 재화 API

### 8.1 내 재화 조회

```http
GET /currency/me
```

Response:

```json
{
  "amount": 100
}
```

### 8.2 재화 지급

미니게임, 출석, 보상 등으로 재화를 지급한다.

```http
POST /currency/earn
```

Request:

```json
{
  "amount": 20,
  "reason": "MINIGAME_REWARD",
  "idempotencyKey": "minigame_result_001"
}
```

Response:

```json
{
  "amount": 120,
  "transaction": {
    "id": "currency_tx_001",
    "type": "EARN",
    "amount": 20,
    "reason": "MINIGAME_REWARD",
    "createdAt": "2026-05-04T20:00:00+09:00"
  }
}
```

비고:

- 같은 보상이 중복 지급되지 않도록 `idempotencyKey` 사용을 권장한다.
- 클라이언트에서 임의로 금액을 보내는 방식은 악용 가능성이 있으므로, 실제 운영에서는 서버가 보상량을 계산하는 방식이 더 안전하다.

### 8.3 재화 차감

상점 구매 등으로 재화를 차감한다.

```http
POST /currency/spend
```

Request:

```json
{
  "amount": 50,
  "reason": "SHOP_PURCHASE",
  "referenceId": "shop_item_001"
}
```

Response:

```json
{
  "amount": 70,
  "transaction": {
    "id": "currency_tx_002",
    "type": "SPEND",
    "amount": 50,
    "reason": "SHOP_PURCHASE",
    "createdAt": "2026-05-04T20:05:00+09:00"
  }
}
```

가능한 에러:

```text
INSUFFICIENT_CURRENCY
INVALID_AMOUNT
```

## 9. 상점 / 인벤토리 API

### 9.1 상점 아이템 목록

```http
GET /shop/items
```

Response:

```json
{
  "items": [
    {
      "id": "shop_item_001",
      "name": "나무 의자",
      "description": "따뜻한 느낌의 나무 의자",
      "price": 50,
      "assetKey": "chair_wood",
      "category": "FURNITURE",
      "isOwned": false
    }
  ]
}
```

아이템 카테고리 예시:

```text
FURNITURE | WALLPAPER | FLOOR | DECORATION | PET_ITEM
```

### 9.2 상점 아이템 구매

```http
POST /shop/items/{itemId}/purchase
```

Response:

```json
{
  "purchasedItem": {
    "id": "inventory_item_001",
    "shopItemId": "shop_item_001",
    "name": "나무 의자",
    "assetKey": "chair_wood",
    "category": "FURNITURE",
    "purchasedAt": "2026-05-04T20:10:00+09:00"
  },
  "currencyAmount": 70
}
```

가능한 에러:

```text
ITEM_NOT_FOUND
ALREADY_PURCHASED
INSUFFICIENT_CURRENCY
```

### 9.3 내 인벤토리 조회

```http
GET /inventory/me
```

Response:

```json
{
  "items": [
    {
      "id": "inventory_item_001",
      "shopItemId": "shop_item_001",
      "name": "나무 의자",
      "assetKey": "chair_wood",
      "category": "FURNITURE",
      "purchasedAt": "2026-05-04T20:10:00+09:00"
    }
  ]
}
```

## 10. 미니게임 API

### 10.1 미니게임 결과 제출

미니게임 종료 후 점수와 결과를 서버에 제출하고 보상을 받는다.

```http
POST /minigames/{gameId}/results
```

Request:

```json
{
  "score": 1200,
  "durationSeconds": 45,
  "clientResultId": "local-result-uuid"
}
```

Response:

```json
{
  "resultId": "minigame_result_001",
  "rewardAmount": 20,
  "currencyAmount": 120,
  "createdAt": "2026-05-04T20:20:00+09:00"
}
```

비고:

- MVP에서는 `gameId` 예시로 `playground` 또는 `jump` 등을 사용할 수 있다.
- 보상 계산은 서버에서 수행하는 것을 권장한다.
- `clientResultId`는 중복 제출 방지용이다.

가능한 에러:

```text
INVALID_GAME
INVALID_SCORE
DUPLICATE_RESULT
```

## 11. 갤러리 API

갤러리 동기화가 MVP에 포함된다면 아래 API를 사용한다.  
단, 스크린샷 이미지 파일 자체를 서버에 올릴지, 기기 로컬에만 둘지는 별도 결정이 필요하다.

### 11.1 갤러리 항목 목록

```http
GET /gallery/items
```

Response:

```json
{
  "items": [
    {
      "id": "gallery_item_001",
      "title": "내 방 스크린샷",
      "imageUrl": "https://cdn.lupalupa.com/gallery/gallery_item_001.png",
      "thumbnailUrl": "https://cdn.lupalupa.com/gallery/gallery_item_001_thumb.png",
      "createdAt": "2026-05-04T21:00:00+09:00"
    }
  ]
}
```

### 11.2 갤러리 항목 등록

```http
POST /gallery/items
```

Request:

```json
{
  "title": "내 방 스크린샷",
  "imageUrl": "https://cdn.lupalupa.com/gallery/gallery_item_001.png",
  "thumbnailUrl": "https://cdn.lupalupa.com/gallery/gallery_item_001_thumb.png"
}
```

Response:

```json
{
  "item": {
    "id": "gallery_item_001",
    "title": "내 방 스크린샷",
    "imageUrl": "https://cdn.lupalupa.com/gallery/gallery_item_001.png",
    "thumbnailUrl": "https://cdn.lupalupa.com/gallery/gallery_item_001_thumb.png",
    "createdAt": "2026-05-04T21:00:00+09:00"
  }
}
```

## 12. Android 앱에서 우선 연결할 순서

MVP 개발에서는 다음 순서로 연결하는 것이 좋다.

1. `POST /auth/kakao`
2. `GET /users/me`
3. `GET /pets/me`
4. `POST /rooms/me/layout/validate`
5. `GET /friends`
6. `GET /friends/requests/received`
7. `GET /friends/requests/sent`
8. `POST /friends/requests`
9. `POST /friends/requests/{requestId}/accept`
10. `POST /friends/requests/{requestId}/reject`
11. `GET /friends/home-invitations/received`
12. `POST /friends/home-invitations/{invitationId}/accept`
13. `POST /friends/home-invitations/{invitationId}/reject`
14. `GET /shop/items`
15. `POST /shop/items/{itemId}/purchase`
16. `GET /inventory/me`
17. `PUT /rooms/me/layout`
18. `PUT /pets/me/equipment`
19. `PATCH /pets/me/status`
20. `POST /minigames/{gameId}/results`

친구 메시지와 보낸 집 초대 목록/초대 취소는 친구 시스템의 기본 CRUD와 우편함 흐름이 안정화된 뒤 연결하는 것을 권장한다.

## 13. 백엔드와 합의해야 할 사항

아래 항목은 구현 전에 백엔드와 먼저 맞춰야 한다.

1. 실제 Base URL
2. access token 만료 시간
3. refresh token 만료 시간
4. 친구 코드 형식
5. 친구 코드 대소문자 구분 여부
6. 친구 요청 거절 후 재신청 가능 여부
7. 친구 삭제 후 재신청 가능 여부
8. 친구 집 방문은 초대 수락 응답의 `homeSnapshot`으로 진입하는 정책
9. 상점 아이템의 `assetKey` 명명 규칙
10. 재화 보상 계산을 클라이언트가 보낼지 서버가 계산할지 여부
11. 갤러리 이미지 파일을 서버에 업로드할지 로컬만 사용할지 여부
12. 실시간 채팅을 MVP에 포함할지 여부
13. 방 레이아웃 충돌 시 서버 우선으로 처리할지 여부
14. 가구 배치를 전체 snapshot으로 저장할지 부분 수정으로 저장할지 여부
15. 타일 겹침 검증을 서버에서 강제할지 여부
16. 펫 외형 랜덤 사이즈 범위
17. 펫 외형을 최초 생성 후 변경 가능하게 할지 여부
18. 펫 성격을 서버가 랜덤 배정할지 사용자가 선택할지 여부
19. 펫 알 상태가 언제 해제되는지에 대한 부화 조건
20. 펫 치장 아이템을 단순 id 배열로 관리할지 부위별 슬롯으로 관리할지 여부
21. 포만감과 활력 수치를 클라이언트가 보낼지 서버가 행동 결과로 계산할지 여부
22. 친구 집 초대 만료 시간을 1시간, 24시간, 또는 만료 없음 중 무엇으로 둘지 여부

## 14. MVP 권장 결정안

현재 앱 완성도를 고려하면 MVP에서는 다음처럼 단순하게 시작하는 것을 권장한다.

- 친구 코드는 서버에서 자동 생성한다.
- 친구 코드는 `LUPA` + 숫자 4자리 또는 6자리로 시작한다.
- 친구 요청은 한 방향에 하나만 유지한다.
- 친구 요청을 수락하면 양방향 친구 관계가 생성된다.
- 친구 요청을 거절하면 같은 유저에게 재신청을 허용한다.
- 친구 삭제 후 재신청을 허용한다.
- 친구 집은 우편함의 집 초대를 수락한 경우에만 진입한다.
- 친구 집 초대 수락 응답은 `homeSnapshot` 하나로 방, 펫 스냅샷, 방문 시각을 묶어서 내려준다.
- 채팅은 실시간 socket 없이 REST 메시지 목록으로 시작한다.
- 상점 구매와 재화 차감은 하나의 트랜잭션으로 서버에서 처리한다.
- 미니게임 보상 금액은 서버에서 계산한다.
- 갤러리 이미지는 MVP 초반에는 로컬 저장을 우선하고, 서버 동기화는 후순위로 둔다.
- 앱 시작 시 `POST /rooms/me/layout/validate`로 방 레이아웃을 검증한다.
- 방 레이아웃이 충돌하면 MVP에서는 서버 상태를 우선한다.
- 가구 배치 변경 시에는 `PUT /rooms/me/layout`로 전체 레이아웃 snapshot을 저장한다.
- `userId`는 서버 내부 식별자로 사용하고, 친구 추가에는 `friendCode`만 사용한다.
- 펫 외형과 성격은 최초 생성 시 서버에서 랜덤으로 정한다.
- 펫 치장 아이템은 MVP에서는 `equippedItemIds` 배열로 관리한다.
- 착용 아이템이 없으면 `null` 대신 빈 배열 `[]`을 사용한다.
- 포만감과 활력은 MVP에서는 `0`부터 `100` 사이 정수로 관리한다.

## 15. 클라이언트 구현 메모

Android 앱에서 API 연동 시 다음 구조를 권장한다.

- `data/api`: Retrofit service interface
- `data/dto`: 서버 request/response DTO
- `data/model`: 앱 내부 도메인 모델
- `data/repository`: API 호출과 DTO 변환 담당
- `ui/viewmodel`: 화면 상태 관리

서버 DTO와 Compose 화면 상태를 직접 연결하지 않고, Repository에서 앱 내부 모델로 변환하면 이후 API 변경에 대응하기 쉽다.
