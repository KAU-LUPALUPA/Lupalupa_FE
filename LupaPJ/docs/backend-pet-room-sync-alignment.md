# 백엔드 요청 문서: 초기 접속 펫/방 레이아웃 동기화 API 정렬

버전: `0.1`  
목적: Android 앱 초기 접속 시 펫 정보와 방 레이아웃을 서버 기준으로 검증/동기화하기 위한 백엔드 API 정렬 요청

## 1. 배경

프론트는 앱 로그인 직후 다음 흐름을 기대하고 있습니다.

1. access token 확보
2. 내 펫 정보 조회 또는 검증
3. 로컬 방 레이아웃 revision/hash 확인
4. 서버에 방 레이아웃 검증 요청
5. 서버가 최신이면 서버 레이아웃을 로컬에 저장하고 화면에 반영
6. 로컬이 최신이면 기존 로컬 레이아웃 그대로 사용

현재 프론트에는 `PetApiClient`, `RoomLayoutApiClient`, DTO/mapper 초안은 있지만 실제 초기 접속 플로우에는 아직 연결되지 않았습니다. 백엔드 API path와 응답 모델이 확정되면 프론트에서 해당 API를 붙일 예정입니다.

## 2. 현재 불일치

현재 문서상 계약과 실제 백엔드 구현이 다릅니다.

| 구분 | 계약서 기준 | 현재 서버 구현 예시 | 이슈 |
|---|---|---|---|
| 펫 조회 | `GET /pets/me` | `/api/pets/{petId}/feed`, `/sleep`, `/play` 중심 | 초기 접속용 내 펫 조회 API가 필요 |
| 펫 검증 | `POST /pets/me/validate` | 없음 또는 미확인 | local revision/hash 비교 API 필요 |
| 방 검증 | `POST /rooms/me/layout/validate` | 없음 또는 미확인 | 초기 접속 핵심 API 필요 |
| 방 조회 | `GET /rooms/me/layout` | `GET /room?petId=1`, `GET /api/room/load/{uid}` | path와 응답 모델 통일 필요 |
| 방 저장 | `PUT /rooms/me/layout` | `POST /api/room/save/{uid}` | 인증 사용자 기준 저장으로 통일 필요 |

가능하면 기존 계약서 기준인 `/pets/me`, `/rooms/me/layout` 계열로 맞추는 것을 요청드립니다.

## 3. 공통 규칙

모든 API는 로그인 사용자 기준입니다.

```http
Authorization: Bearer {accessToken}
Content-Type: application/json
```

서버는 path의 `uid`, `petId`를 프론트에서 받기보다, 토큰에서 추출한 `currentUid` 기준으로 내 펫/내 방을 조회하는 방향이 좋습니다.

에러 응답은 아래 형태로 통일하면 프론트 분기가 쉽습니다.

```json
{
  "code": "ROOM_LAYOUT_CONFLICT",
  "message": "서버에 더 최신 레이아웃이 있습니다."
}
```

## 4. 필요한 API 목록

### 4.1 내 펫 조회

초기 접속 시 서버의 현재 펫 상태를 가져옵니다.

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
      "isEgg": false
    },
    "personality": "ACTIVE",
    "equippedItemIds": [],
    "action": "IDLE",
    "anchor": {
      "u": 0.44,
      "v": 0.64
    },
    "petRevision": 1,
    "petHash": "pet_hash_abc",
    "updatedAt": "2026-05-12T21:30:00+09:00"
  }
}
```

필드 매핑 주의:

| 프론트 필드 | 의미 | 현재 서버 필드와 관계 |
|---|---|---|
| `satiety` | 포만감 | 기존 `hunger`와 같은 의미로 맞추는 것을 권장 |
| `vitality` | 활력 | 기존 `stamina`와 같은 의미로 맞추는 것을 권장 |
| `action` | 현재 행동 | `IDLE`, `WALKING`, `RESTING`, `BED_RESTING`, `PLAYING`, `EATING` |
| `anchor` | 방 안 펫 위치 | 정규화 좌표 `u`, `v`, 범위 `0.0~1.0` |

### 4.2 내 펫 검증

프론트가 로컬에 저장한 펫 revision/hash를 서버와 비교합니다.

```http
POST /pets/me/validate
```

Request:

```json
{
  "localPetRevision": 1,
  "localPetHash": "pet_hash_abc",
  "localUpdatedAt": "2026-05-12T21:20:00+09:00"
}
```

Response - 로컬과 서버가 같을 때:

```json
{
  "syncStatus": "MATCH",
  "serverPetRevision": 1,
  "serverPetHash": "pet_hash_abc",
  "serverUpdatedAt": "2026-05-12T21:20:00+09:00",
  "pet": null
}
```

Response - 서버가 최신일 때:

```json
{
  "syncStatus": "SERVER_NEWER",
  "serverPetRevision": 2,
  "serverPetHash": "pet_hash_def",
  "serverUpdatedAt": "2026-05-12T21:30:00+09:00",
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
      "isEgg": false
    },
    "personality": "ACTIVE",
    "equippedItemIds": [],
    "action": "IDLE",
    "anchor": {
      "u": 0.44,
      "v": 0.64
    },
    "petRevision": 2,
    "petHash": "pet_hash_def",
    "updatedAt": "2026-05-12T21:30:00+09:00"
  }
}
```

초기 MVP에서는 펫 검증이 부담되면 `GET /pets/me`만 먼저 구현해도 됩니다. 다만 응답에는 `petRevision`, `petHash`, `updatedAt`을 포함해두면 이후 검증 API 확장이 쉽습니다.

### 4.3 방 레이아웃 검증

초기 접속 시 가장 중요한 API입니다. 프론트가 로컬 방 레이아웃 메타데이터를 보내면 서버가 최신 여부를 판단합니다.

```http
POST /rooms/me/layout/validate
```

Request:

```json
{
  "localLayoutRevision": 3,
  "localLayoutHash": "layout_hash_abc",
  "localUpdatedAt": "2026-05-12T21:20:00+09:00"
}
```

Response - 로컬과 서버가 같을 때:

```json
{
  "syncStatus": "MATCH",
  "serverLayoutRevision": 3,
  "serverLayoutHash": "layout_hash_abc",
  "serverUpdatedAt": "2026-05-12T21:20:00+09:00",
  "roomLayout": null
}
```

Response - 서버가 최신일 때:

```json
{
  "syncStatus": "SERVER_NEWER",
  "serverLayoutRevision": 4,
  "serverLayoutHash": "layout_hash_def",
  "serverUpdatedAt": "2026-05-12T21:30:00+09:00",
  "roomLayout": {
    "roomId": "room_123",
    "ownerUserId": "user_123",
    "sceneId": "main_room",
    "layoutRevision": 4,
    "layoutHash": "layout_hash_def",
    "wallAssetKey": "room/walls/main_wall",
    "floorAssetKey": "room/floors/main_floor",
    "placedItems": [
      {
        "placementId": "placed_bed_001",
        "inventoryItemId": null,
        "shopItemId": "bed_basic",
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
          "anchorMode": "CENTER"
        },
        "wallPlacement": null,
        "scale": 1.0,
        "rotation": 0,
        "depthBias": 0.0
      }
    ],
    "updatedAt": "2026-05-12T21:30:00+09:00"
  }
}
```

권장 `syncStatus` 값:

| 값 | 의미 | 프론트 처리 |
|---|---|---|
| `MATCH` | 로컬과 서버가 같음 | 로컬 방 사용 |
| `SERVER_NEWER` | 서버가 더 최신 | 응답의 `roomLayout` 저장 후 적용 |
| `SERVER_ONLY` | 로컬 데이터가 없음 | 응답의 `roomLayout` 저장 후 적용 |
| `CONFLICT` | 양쪽이 다르게 변경됨 | MVP에서는 서버 상태 우선 적용 |

### 4.4 내 방 레이아웃 조회

검증 없이 서버 최신 레이아웃을 직접 가져올 때 사용합니다.

```http
GET /rooms/me/layout
```

Response:

```json
{
  "roomLayout": {
    "roomId": "room_123",
    "ownerUserId": "user_123",
    "sceneId": "main_room",
    "layoutRevision": 4,
    "layoutHash": "layout_hash_def",
    "wallAssetKey": "room/walls/main_wall",
    "floorAssetKey": "room/floors/main_floor",
    "placedItems": [],
    "updatedAt": "2026-05-12T21:30:00+09:00"
  }
}
```

### 4.5 내 방 레이아웃 저장

가구 배치 변경 후 전체 snapshot을 저장합니다.

```http
PUT /rooms/me/layout
```

Request:

```json
{
  "baseLayoutRevision": 4,
  "wallAssetKey": "room/walls/main_wall",
  "floorAssetKey": "room/floors/main_floor",
  "placedItems": []
}
```

Response:

```json
{
  "roomLayout": {
    "roomId": "room_123",
    "ownerUserId": "user_123",
    "sceneId": "main_room",
    "layoutRevision": 5,
    "layoutHash": "layout_hash_new",
    "wallAssetKey": "room/walls/main_wall",
    "floorAssetKey": "room/floors/main_floor",
    "placedItems": [],
    "updatedAt": "2026-05-12T21:35:00+09:00"
  }
}
```

저장 시 권장 검증:

- `baseLayoutRevision`이 서버의 현재 `layoutRevision`과 다르면 `409 ROOM_LAYOUT_CONFLICT`
- 같은 타일에 여러 가구가 겹치면 `400 TILE_ALREADY_OCCUPIED`
- 유저가 소유하지 않은 인벤토리 아이템이면 `403 ITEM_NOT_OWNED`

## 5. 백엔드 구현 요청 사항

우선순위 기준으로 정리하면 다음과 같습니다.

1. `GET /pets/me` 구현
2. `POST /rooms/me/layout/validate` 구현
3. `GET /rooms/me/layout` 구현
4. `PUT /rooms/me/layout` 구현
5. 필요 시 `POST /pets/me/validate`, `PATCH /pets/me/status`, `PUT /pets/me/equipment` 추가

초기 프론트 연결을 위해 최소로 필요한 것은 `GET /pets/me`와 `POST /rooms/me/layout/validate`입니다.

## 6. 서버 데이터 모델에 필요한 필드

펫 테이블 또는 응답 계산에 아래 메타데이터가 필요합니다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `petRevision` | int | 펫 상태가 바뀔 때 증가 |
| `petHash` | string | 펫 주요 필드 기반 hash |
| `updatedAt` | datetime | 마지막 수정 시각 |
| `anchorU`, `anchorV` | float | 펫 위치 |
| `satiety`, `vitality` | int | 프론트 용어 기준 상태값 |

방 레이아웃에는 아래 메타데이터가 필요합니다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `layoutRevision` | int | 레이아웃 저장 때마다 증가 |
| `layoutHash` | string | 레이아웃 snapshot 기반 hash |
| `updatedAt` | datetime | 마지막 수정 시각 |
| `sceneId` | string | 기본값 `main_room` |
| `placedItems` | array | 배치된 가구/장식 전체 snapshot |

## 7. 프론트 연결 예정 흐름

백엔드 API가 확정되면 프론트는 다음 순서로 붙일 예정입니다.

1. `AppContainer`에서 `MockRoomRepository` 대신 remote repository 주입
2. 로그인 성공 후 `GET /pets/me` 호출
3. 로컬 DataStore에서 `localLayoutRevision`, `localLayoutHash`, `localUpdatedAt` 읽기
4. `POST /rooms/me/layout/validate` 호출
5. `MATCH`면 기존 로컬 방 사용
6. `SERVER_NEWER`, `SERVER_ONLY`, `CONFLICT`면 서버 `roomLayout`을 로컬에 저장하고 화면 적용

## 8. 논의가 필요한 결정

아래는 백엔드/프론트가 먼저 합의해야 합니다.

| 항목 | 제안 |
|---|---|
| API prefix | `/pets/me`, `/rooms/me/layout` 사용. 기존 `/api/pets`, `/api/room`은 내부/구버전으로 정리 |
| 펫 상태명 | 서버 응답은 `satiety`, `vitality`로 통일. 기존 DB 필드가 `hunger`, `stamina`여도 DTO에서 변환 |
| 방 저장 방식 | 부분 수정이 아니라 전체 snapshot 저장 |
| 충돌 정책 | MVP에서는 서버 우선 |
| ID 기준 | path에 `uid`, `petId`를 받지 않고 token의 `currentUid` 기준 |
| 시간 형식 | ISO-8601 문자열 |

