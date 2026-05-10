# 루파루파 친구 집 초대 API 계약서

버전: `0.1`  
상태: 신규 초대 기반 방문 플로우 초안  
기준일: 2026-05-09  
범위: 친구 집 초대 생성, 받은 초대 조회, 보낸 초대 조회, 초대 수락/거절/취소, 초대 수락 후 친구 집 방문 데이터 반환

## 변경 배경

기존 친구 시스템에는 친구 목록에서 바로 친구 집으로 접근하는 방식이 있었다.

새 정책에서는 친구 관계만으로는 친구 집에 바로 방문할 수 없다.
집 주인이 초대를 보내고, 초대 수신자가 수락한 경우에만 친구 집 화면으로 진입한다.

통합 계약서와 친구 API 계약서에서는 기존 방문 방식을 제거하고, 이 문서를 친구 집 방문 방식의 신규 초대 계약으로 관리한다.

## 공통 규칙

| 항목 | 내용 |
|---|---|
| Base URL | 기존 친구 API와 동일 |
| 인증 | 모든 API는 `Authorization: Bearer {accessToken}` 필요 |
| Content-Type | `application/json` |
| 시간 형식 | ISO-8601 문자열. 예: `2026-05-09T18:30:00+09:00` |
| 공통 에러 Response | `{ "code": "ERROR_CODE", "message": "사용자 표시 메시지" }` |

## API 목록

| 기능 | 사용자 | Method | URL | param | 설명 | Response | 비고 |
|---|---|---|---|---|---|---|---|
| 친구 집 초대 보내기 | 집 주인 | POST | `/friends/home-invitations` | Body: `friendUserId`, `message?` | 친구에게 내 집 방문 초대를 보낸다. | `invitation` | 친구 관계가 아니면 `NOT_FRIENDS` |
| 받은 집 초대 목록 | 초대 수신자 | GET | `/friends/home-invitations/received` | 없음 | 내가 받은 대기 중인 집 초대 목록을 조회한다. | `invitations[]` | 우편함에서 사용 |
| 보낸 집 초대 목록 | 초대 발신자 | GET | `/friends/home-invitations/sent` | 없음 | 내가 보낸 집 초대 목록을 조회한다. | `invitations[]` | MVP에서는 선택 API |
| 집 초대 수락 및 방문 | 초대 수신자 | POST | `/friends/home-invitations/{invitationId}/accept` | Path: `invitationId` | 받은 집 초대를 수락하고 친구 집 방문 데이터를 받는다. | `invitation`, `homeSnapshot{owner, room, petSnapshot, snapshotAt, visitedAt}` | 친구 집 화면 진입 API |
| 집 초대 거절 | 초대 수신자 | POST | `/friends/home-invitations/{invitationId}/reject` | Path: `invitationId` | 받은 집 초대를 거절한다. | `invitation` | 우편함에서 제거 |
| 보낸 집 초대 취소 | 초대 발신자 | POST | `/friends/home-invitations/{invitationId}/cancel` | Path: `invitationId` | 아직 수락되지 않은 보낸 초대를 취소한다. | `invitation` | MVP에서는 선택 API |

## 권장 플로우

### 1. 초대 보내기

친구에게 내 집 방문 초대를 보낼 때 호출한다.

초대 발신 UI의 진입점은 아직 확정하지 않는다.  
현재 합의된 프론트 범위는 받은 초대를 우편함에서 확인하고 수락/거절하는 흐름이다.

```http
POST /friends/home-invitations
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "friendUserId": "user_friend",
  "message": "우리 집 구경하러 올래?"
}
```

```json
{
  "invitation": {
    "id": "home_invitation_001",
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
    "message": "우리 집 구경하러 올래?",
    "createdAt": "2026-05-09T18:30:00+09:00",
    "respondedAt": null,
    "expiresAt": "2026-05-09T19:30:00+09:00"
  }
}
```

### 2. 우편함에서 받은 집 초대 조회

앱 진입, 친구 시스템 새로고침, 우편함 열기 시 호출한다.

```http
GET /friends/home-invitations/received
Authorization: Bearer {accessToken}
```

```json
{
  "invitations": [
    {
      "id": "home_invitation_001",
      "fromUser": {
        "userId": "user_friend",
        "nickname": "친구루파",
        "friendCode": "LUPA5B0RI",
        "displayFriendCode": "LUPA-5B0RI",
        "avatarAssetKey": null
      },
      "toUser": {
        "userId": "user_me",
        "nickname": "나의루파",
        "friendCode": "LUPAME01",
        "displayFriendCode": "LUPA-ME01",
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

### 3. 집 초대 수락 및 친구 집 방문

수신자가 우편함에서 `방문`을 누르면 호출한다.  
성공 응답은 친구 집 화면을 그리는 데 필요한 `homeSnapshot`을 반환한다.  
`homeSnapshot`은 방문 시점의 읽기 전용 데이터 묶음이며, 방문자는 친구의 `room` 또는 `petSnapshot`을 수정할 수 없다.

```http
POST /friends/home-invitations/{invitationId}/accept
Authorization: Bearer {accessToken}
```

```json
{
  "invitation": {
    "id": "home_invitation_001",
    "status": "ACCEPTED",
    "respondedAt": "2026-05-09T18:35:00+09:00"
  },
  "homeSnapshot": {
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
      "updatedAt": "2026-05-09T18:30:00+09:00"
    },
    "petSnapshot": {
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

### 응답 구조 선택 이유

| 구조 | 이유 |
|---|---|
| `homeSnapshot` | 친구 집 방문 화면에 필요한 데이터를 하나의 읽기 전용 묶음으로 다룬다. |
| `petSnapshot` | 친구 펫을 수정하는 API 응답이 아니라 방문 화면 렌더링용 스냅샷임을 명확히 한다. |
| `condition` | `satiety`, `vitality`, `isEgg`처럼 펫의 컨디션 값을 묶는다. |
| `sceneState` | `action`, `anchor`처럼 현재 방문 화면에서의 위치/행동 값을 묶는다. |

피해야 할 형태:

```json
{
  "petSnapshot": {
    "status": {
      "satiety": 80,
      "vitality": 75,
      "isEgg": false,
      "action": "IDLE",
      "anchor": {
        "u": 0.44,
        "v": 0.64
      }
    }
  }
}
```

### 4. 집 초대 거절

```http
POST /friends/home-invitations/{invitationId}/reject
Authorization: Bearer {accessToken}
```

```json
{
  "invitation": {
    "id": "home_invitation_001",
    "fromUser": {
      "userId": "user_friend",
      "nickname": "친구루파",
      "friendCode": "LUPA5B0RI",
      "displayFriendCode": "LUPA-5B0RI",
      "avatarAssetKey": null
    },
    "toUser": {
      "userId": "user_me",
      "nickname": "나의루파",
      "friendCode": "LUPAME01",
      "displayFriendCode": "LUPA-ME01",
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

## 데이터 모델

### AcceptHomeInvitationResponse

집 초대 수락 API의 성공 응답이다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `invitation` | `FriendHomeInvitation` | 수락 처리된 초대 정보 |
| `homeSnapshot` | `FriendHomeSnapshot` | 친구 집 방문 화면을 그리기 위한 읽기 전용 스냅샷 |

### FriendHomeInvitation

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | `String` | 집 초대 ID |
| `fromUser` | `FriendUser` | 초대를 보낸 유저. 집 주인 |
| `toUser` | `FriendUser` | 초대를 받은 유저 |
| `status` | `FriendHomeInvitationStatus` | `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELED`, `EXPIRED` |
| `message` | `String?` | 초대 메시지. 없으면 `null` |
| `createdAt` | `String` | 초대 생성 시각 |
| `respondedAt` | `String?` | 수락/거절/취소 시각 |
| `expiresAt` | `String?` | 초대 만료 시각. 만료 정책이 없으면 `null` 가능 |

### FriendHomeSnapshot

친구 집 방문 화면을 그리기 위한 읽기 전용 데이터 묶음이다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `owner` | `FriendUser` | 방문 대상 집 주인 |
| `room` | `FriendRoomSnapshot` | 방문 시점의 방 레이아웃 스냅샷 |
| `petSnapshot` | `FriendPetSnapshot?` | 방문 시점의 펫 스냅샷. 표시할 펫이 없으면 `null` |
| `snapshotAt` | `String` | 서버가 스냅샷을 생성한 시각 |
| `visitedAt` | `String` | 방문이 성립된 시각 |

### FriendPetSnapshot

친구 집 방문 화면에서 보여줄 펫의 읽기 전용 스냅샷이다.  
방문자가 이 값을 수정하거나 저장하지 않는다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `petId` | `String` | 펫 ID |
| `ownerUserId` | `String` | 펫 주인 유저 ID |
| `name` | `String` | 펫 이름 |
| `characterAssetKey` | `String` | 캐릭터 기본 에셋 키 |
| `appearance` | `FriendPetAppearanceSnapshot` | 외형 비율 묶음 |
| `condition` | `FriendPetConditionSnapshot` | 포만감, 활력, 알 상태 등 컨디션 묶음 |
| `sceneState` | `FriendPetSceneStateSnapshot` | 방문 화면에서의 현재 행동과 위치 묶음 |
| `personality` | `String` | `ACTIVE`, `CALM`, `LAZY` |
| `equippedItemIds` | `String[]` | 착용 중인 치장 아이템 ID 목록 |

### FriendPetAppearanceSnapshot

| 필드 | 타입 | 설명 |
|---|---|---|
| `headSizeScale` | `Float` | 머리 크기 비율 |
| `bodySizeScale` | `Float` | 몸 크기 비율 |
| `eyeSizeScale` | `Float` | 눈 크기 비율 |
| `noseSizeScale` | `Float` | 코 크기 비율 |
| `mouthSizeScale` | `Float` | 입 크기 비율 |

### FriendPetConditionSnapshot

| 필드 | 타입 | 설명 |
|---|---|---|
| `satiety` | `Int` | 포만감. `0~100`. 높을수록 배부른 상태 |
| `vitality` | `Int` | 활력. `0~100`. 높을수록 활기찬 상태 |
| `isEgg` | `Boolean` | 알 상태 여부 |

### FriendPetSceneStateSnapshot

| 필드 | 타입 | 설명 |
|---|---|---|
| `action` | `String` | 방문 화면에서 보여줄 현재 행동. 예: `IDLE`, `RESTING`, `PLAYING` |
| `anchor` | `FriendAnchor` | 방문 화면에서 보여줄 현재 위치 |

### FriendHomeInvitationStatus

| 값 | 설명 |
|---|---|
| `PENDING` | 아직 처리되지 않은 초대 |
| `ACCEPTED` | 수신자가 초대를 수락함 |
| `REJECTED` | 수신자가 초대를 거절함 |
| `CANCELED` | 발신자가 초대를 취소함 |
| `EXPIRED` | 서버 정책에 의해 초대가 만료됨 |

## 에러 코드

| code | HTTP | 설명 |
|---|---:|---|
| `FRIEND_NOT_FOUND` | `404` | 대상 친구 정보를 찾을 수 없음 |
| `NOT_FRIENDS` | `403` | 친구 관계가 아니어서 초대를 보내거나 수락할 수 없음 |
| `HOME_INVITATION_ALREADY_SENT` | `409` | 같은 친구에게 이미 대기 중인 집 초대가 있음 |
| `HOME_INVITATION_NOT_FOUND` | `404` | 집 초대를 찾을 수 없음 |
| `HOME_INVITATION_NOT_PENDING` | `409` | 이미 수락/거절/취소/만료된 초대 |
| `NOT_HOME_INVITATION_RECEIVER` | `403` | 초대 수신자가 아니라 수락/거절할 수 없음 |
| `NOT_HOME_INVITATION_SENDER` | `403` | 초대 발신자가 아니라 취소할 수 없음 |
| `FRIEND_HOME_UNAVAILABLE` | `503` | 친구 집 정보를 불러올 수 없음 |
| `BLOCKED` | `403` | 차단 관계 또는 정책상 초대를 보낼 수 없음 |

## 정책 합의 사항

| 항목 | 권장 정책 |
|---|---|
| 방문 방식 | 초대 수락 API 응답으로만 친구 집 화면에 진입 |
| 중복 초대 | 같은 발신자와 수신자 사이에 `PENDING` 초대는 한 개만 허용 |
| 초대 만료 | MVP에서는 1시간 또는 24시간 중 택일. 미정이면 `expiresAt = null` 허용 |
| 초대 수락 후 재방문 | 수락 응답으로 받은 친구 집 화면에 진입한다. 화면 이탈 후 재방문은 새 초대가 필요 |
| 친구 삭제 시 초대 | 두 유저의 대기 중 초대는 서버에서 `CANCELED` 또는 삭제 처리 |
| 우편함 표시 | 프론트는 `GET /friends/home-invitations/received`의 `PENDING` 항목을 우편함에 표시 |

## 프론트 연동 메모

현재 Android 프론트에서 예상하는 메서드 이름은 다음과 같다.

| 프론트 메서드 | API |
|---|---|
| `getReceivedHomeInvitations()` | `GET /friends/home-invitations/received` |
| `acceptHomeInvitation(invitationId)` | `POST /friends/home-invitations/{invitationId}/accept` |
| `rejectHomeInvitation(invitationId)` | `POST /friends/home-invitations/{invitationId}/reject` |

추후 친구에게 초대를 보내는 UI가 추가되면 다음 API를 연결한다.

| 프론트 예상 메서드 | API |
|---|---|
| `sendHomeInvitation(friendUserId, message?)` | `POST /friends/home-invitations` |
| `getSentHomeInvitations()` | `GET /friends/home-invitations/sent` |
| `cancelHomeInvitation(invitationId)` | `POST /friends/home-invitations/{invitationId}/cancel` |
