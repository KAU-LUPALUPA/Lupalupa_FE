# 백엔드 논의 문서: 펫/방 모델 정렬

버전: `0.1`  
목적: 프론트와 백엔드가 펫/방 데이터를 같은 의미로 주고받기 위한 모델 정렬 논의

## 1. 핵심 결론

현재 백엔드는 펫과 방을 "기본 게임 데이터" 중심으로 저장하고 있고, 프론트는 화면을 복원할 수 있는 "렌더링용 스냅샷"을 기대하고 있습니다.

따라서 백엔드는 DB 엔티티를 바로 전부 바꾸지 않아도 되지만, API 응답 DTO는 프론트가 이해할 수 있는 형태로 맞춰주는 것이 필요합니다.

MVP에서는 아래 방향을 제안합니다.

| 항목 | MVP 제안 |
|---|---|
| 펫 외형 배율 | 서버에서 안 내려줘도 됨. 프론트 기본값 사용 |
| 펫 상태명 | API 응답에서는 `satiety`, `vitality`로 통일 |
| 펫 위치 | 서버가 없으면 기본 `anchor`를 내려주거나 프론트 기본값 사용 |
| 펫 행동 상태 | 서버/프론트 enum 값 합의 필요 |
| 방 가구 배치 | 최소 `type`, `tile x/y`, `footprint`, `assetKey` 필요 |
| 동기화 메타데이터 | 최소 `layoutRevision`, `updatedAt` 필요. `layoutHash`는 후순위 가능 |

## 2. 펫 모델 정렬

### 2.1 현재 차이

현재 백엔드 루트 서버의 펫 모델은 대략 아래와 같습니다.

```text
id
user
name
personality
hunger
stamina
currentAction
```

프론트가 화면에서 사용하려는 펫 모델은 아래에 가깝습니다.

```text
petId
ownerUserId
name
characterAssetKey
appearance
status.satiety
status.vitality
status.isEgg
personality
equippedItemIds
action
anchor.u
anchor.v
petRevision
petHash
updatedAt
```

모든 필드를 MVP에서 바로 서버가 저장할 필요는 없습니다.  
다만 API 응답에서 어떤 값은 서버가 주고, 어떤 값은 프론트 기본값으로 처리할지 정해야 합니다.

### 2.2 필드별 합의 제안

| 프론트 필드 | 현재 백엔드 대응 | MVP 처리 제안 |
|---|---|---|
| `petId` | `Pet.id` | 문자열로 변환해서 응답. 예: `"pet_1"` 또는 `"1"` |
| `ownerUserId` | `User.uid` | DB PK가 아니라 `uid` 기준 권장 |
| `name` | `Pet.name` | 그대로 사용 |
| `characterAssetKey` | 없음 또는 별도 필요 | 없으면 `"room/characters/lupa_default"` 기본값 |
| `appearance` | 루트 서버에는 없음, roomlook에는 유사 필드 있음 | MVP에서는 optional. 서버가 안 주면 프론트 기본값 |
| `satiety` | `hunger` | API 응답에서 `hunger -> satiety` 변환 |
| `vitality` | `stamina` | API 응답에서 `stamina -> vitality` 변환 |
| `isEgg` | 없음 | MVP 기본값 `false` |
| `personality` | `Pet.personality` | `ACTIVE`, `CALM`, `LAZY` 중 하나 |
| `equippedItemIds` | 아직 없음 | MVP 기본값 `[]` |
| `action` | `currentAction` | enum 값 합의 필요 |
| `anchor` | 없음 | MVP 기본값 `{ "u": 0.44, "v": 0.64 }` 가능 |
| `petRevision` | 없음 | 후순위. 가능하면 int 추가 |
| `petHash` | 없음 | 후순위. revision 먼저 가능 |
| `updatedAt` | 없음 | 가능하면 추가 |

### 2.3 펫 외형 배율은 MVP에서 optional

현재는 펫 외형 배율을 서버에서 받지 않고 구현 중이므로, MVP에서는 서버가 `appearance`를 내려주지 않아도 됩니다.

프론트 처리 방식:

```text
appearance가 있으면 서버 값 사용
appearance가 없으면 프론트 기본 PetAppearance() 사용
```

프론트 DTO도 아래처럼 nullable로 둘 예정입니다.

```kotlin
data class PetDto(
    val petId: String,
    val ownerUserId: String,
    val name: String,
    val characterAssetKey: String? = null,
    val appearance: PetAppearanceDto? = null,
    val status: PetStatusDto,
    val personality: String? = null,
    val equippedItemIds: List<String> = emptyList(),
    val action: String? = null,
    val anchor: PetAnchorDto? = null,
    val petRevision: Int? = null,
    val petHash: String? = null,
    val updatedAt: String? = null
)
```

백엔드에 요청할 말:

> 펫 외형 배율은 지금 서버에서 안 내려줘도 괜찮습니다. 프론트에서 기본 외형으로 렌더링하고, 나중에 커스터마이징이 들어갈 때 `appearance` 필드를 optional로 추가해서 받겠습니다.

### 2.4 펫 상태명 정리

현재 서버 용어:

```text
hunger
stamina
```

프론트 용어:

```text
satiety
vitality
```

뜻은 거의 아래처럼 매핑됩니다.

```text
hunger  -> satiety
stamina -> vitality
```

DB 필드는 당장 바꾸지 않아도 됩니다.  
대신 API DTO에서는 프론트 용어인 `satiety`, `vitality`로 내려주는 것을 제안합니다.

### 2.5 행동 상태 enum 합의

현재 서버는 `SLEEPING`을 사용하고, 프론트는 현재 아래 값을 사용합니다.

```text
IDLE
WALKING
RESTING
BED_RESTING
PLAYING
EATING
```

논의가 필요한 선택지:

| 선택 | 설명 |
|---|---|
| A | 서버도 `RESTING`, `BED_RESTING` 기준으로 맞춘다 |
| B | 프론트에 `SLEEPING`을 추가한다 |
| C | 서버 내부는 `SLEEPING`, API 응답 DTO에서 `BED_RESTING`으로 변환한다 |

MVP에서는 C가 가장 부담이 적습니다. 서버 DB/로직은 유지하되, 프론트 응답만 맞출 수 있습니다.

## 3. 방 모델 정렬

### 3.1 현재 차이

현재 루트 서버 방 모델:

```text
Room
- id
- petId
- wallType
- floorTileType

RoomFurniture
- id
- type
- x
- y
- direction
- status
- roomId
```

프론트가 기대하는 방 레이아웃 모델:

```text
roomId
ownerUserId
sceneId
layoutRevision
layoutHash
wallAssetKey
floorAssetKey
placedItems[]
updatedAt
```

프론트의 `placedItems[]`는 아래 정보를 기대합니다.

```text
placementId
inventoryItemId
shopItemId
assetKey
type
anchorType
tilePlacement.tile.x
tilePlacement.tile.y
tilePlacement.footprint.widthTiles
tilePlacement.footprint.depthTiles
tilePlacement.anchorMode
wallPlacement
scale
rotation
depthBias
```

### 3.2 방 필드별 합의 제안

| 프론트 필드 | 현재 백엔드 대응 | MVP 처리 제안 |
|---|---|---|
| `roomId` | `Room.id` | 문자열로 변환 |
| `ownerUserId` | `User.uid` | 응답에 포함 권장 |
| `sceneId` | 없음 | 기본값 `"main_room"` |
| `layoutRevision` | 없음 | 가능하면 추가. 최소 동기화에 필요 |
| `layoutHash` | 없음 | MVP 후순위 가능 |
| `wallAssetKey` | `wallType` | API 응답에서 asset key 형태로 변환 |
| `floorAssetKey` | `floorTileType` | API 응답에서 asset key 형태로 변환 |
| `placedItems` | `RoomFurniture` 목록 | DTO 변환 필요 |
| `updatedAt` | 없음 | 가능하면 추가 |

### 3.3 가구 배치 필드별 합의 제안

| 프론트 필드 | 현재 백엔드 대응 | MVP 처리 제안 |
|---|---|---|
| `placementId` | `RoomFurniture.id` | 예: `"placed_1"` 또는 `"1"` |
| `type` | `RoomFurniture.type` | `BED`, `TOY_BOX`, `FOOD_BAG`처럼 프론트 enum과 맞추기 |
| `tile.x` | `RoomFurniture.x` | 그대로 매핑 가능 |
| `tile.y` | `RoomFurniture.y` | 그대로 매핑 가능 |
| `rotation` | `direction` | 그대로 또는 변환 |
| `assetKey` | 없음 | type별 기본 asset key 매핑 필요 |
| `footprint` | 루트 서버 없음, roomlook `width/height` 있음 | 기본 가구별 width/depth 매핑 필요 |
| `anchorType` | 없음 | 바닥 가구는 `"FLOOR"` |
| `wallPlacement` | 없음 | MVP에서는 null |
| `scale` | 없음 | 기본값 `1.0` |
| `depthBias` | 없음 | 기본값 `0.0` |

### 3.4 기본 가구 assetKey 예시

MVP에서 최소한 아래 매핑은 필요합니다.

| 서버 type | 프론트 type | assetKey | footprint 예시 |
|---|---|---|---|
| `bed` | `BED` | `room/objects/bed_basic` | `2 x 2` |
| `toy_box` | `TOY_BOX` | `room/objects/toy_box_basic` | `1 x 1` |
| `feed_bag` | `FOOD_BAG` | `room/objects/food_bag_basic` | `1 x 1` |

서버가 assetKey를 DB에 저장하지 않아도, DTO 변환 시 type별로 매핑해서 내려줄 수 있습니다.

## 4. MVP 응답 예시

### 4.1 펫 응답 예시

`appearance`, `petHash`는 MVP에서 없어도 됩니다.

```json
{
  "pet": {
    "petId": "pet_1",
    "ownerUserId": "user_abcd1234",
    "name": "루파",
    "characterAssetKey": "room/characters/lupa_default",
    "appearance": null,
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
    "petHash": null,
    "updatedAt": "2026-05-12T21:30:00+09:00"
  }
}
```

### 4.2 방 레이아웃 응답 예시

```json
{
  "roomLayout": {
    "roomId": "room_1",
    "ownerUserId": "user_abcd1234",
    "sceneId": "main_room",
    "layoutRevision": 1,
    "layoutHash": null,
    "wallAssetKey": "room/walls/main_wall",
    "floorAssetKey": "room/floors/main_floor",
    "placedItems": [
      {
        "placementId": "placed_1",
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

## 5. 백엔드와 논의할 질문

아래 질문에 답이 정해지면 프론트 구현이 바로 가능합니다.

1. 펫 API 응답에서 `hunger/stamina` 대신 `satiety/vitality`로 내려줄 수 있는가?
2. 펫 외형 배율은 MVP에서 서버가 내려주지 않고, 프론트 기본값으로 처리해도 되는가?
3. 펫 위치 `anchor.u`, `anchor.v`를 서버에 저장할 것인가, 아니면 기본값만 응답할 것인가?
4. 행동 상태에서 `SLEEPING`을 어떻게 처리할 것인가?
5. 방 레이아웃에 `layoutRevision`, `updatedAt`을 추가할 수 있는가?
6. `layoutHash`는 MVP에서 null로 두고 추후 추가해도 되는가?
7. 가구 type별 `assetKey`, `footprint`를 서버 DTO에서 매핑해 내려줄 수 있는가?
8. 루트 서버의 `RoomFurniture`와 roomlook의 `Furniture` 중 어느 모델을 기준으로 통합할 것인가?

## 6. 백엔드에 전달할 요약 문장

> 프론트는 방과 펫을 화면에 복원하기 위해 단순 상태값보다 렌더링용 스냅샷이 필요합니다. MVP에서는 펫 외형 배율은 서버에서 안 내려줘도 되고 프론트 기본값을 쓰겠습니다. 대신 API 응답에서 `hunger/stamina`는 `satiety/vitality`로 맞추고, 방 레이아웃은 `roomId`, `ownerUserId`, `sceneId`, `layoutRevision`, `placedItems` 형태로 내려주면 프론트에서 초기 접속 동기화를 붙일 수 있습니다.

