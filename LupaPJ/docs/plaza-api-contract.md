# 루파루파 광장 API 계약서

버전: `0.1`  
상태: MVP 백엔드 연동용 초안  
기준일: 2026-05-11  
범위: 광장 랜덤 입장, 광장 코드 입장, 광장 퇴장, 광장 스냅샷 조회, 광장 채팅, 광장 펫 위치/이동/상호작용 동기화

## 변경 배경

광장은 여러 유저가 동시에 접속하는 공용 공간이다.

한 광장에는 최대 4명까지만 입장할 수 있으며, 모든 유저가 하나의 전역 광장에 들어가는 방식이 아니라 여러 개의 광장 방으로 나뉜다.

유저는 광장 진입 시 다음 중 하나를 선택한다.

- 랜덤 광장 입장
- 친구에게 공유받은 광장 코드로 특정 광장 입장

광장 안에서는 채팅이 가능하고, 접속한 유저들의 펫이 같은 공간 안에서 움직이며 상호작용한다.

## 핵심 정책

| 항목 | 정책 |
|---|---|
| 광장 정원 | 광장 1개당 최대 4명 |
| 광장 코드 | 서버 저장값은 `PZ[A-Z0-9]{4,6}` 형식. 화면 표시값은 `PZ-1234`처럼 하이픈 포함 가능 |
| 위치 좌표 | `0.0~1.0` 정규화 좌표. 현재 프론트 이동 가능 범위는 `x=0.14~0.86`, `y=0.47~0.88` |
| 서버 권위 | 실제 API 연동 광장은 서버가 참가자, 위치, 이동, 상호작용, 채팅, 정원을 최종 결정한다 |
| 클라이언트 역할 | 서버가 내려준 `position`, `movement`, `interaction`을 기준으로 화면 애니메이션만 보간한다 |
| MVP 실시간 방식 | WebSocket으로 `PLAZA_SNAPSHOT` 전체 스냅샷을 broadcast한다 |
| WebSocket 임시 대체 | WebSocket 구현이 늦을 경우 `GET /plazas/{plazaId}`를 `2000ms` 간격으로 polling한다 |
| 시간 단위 | 광장 동기화 시간은 epoch millisecond `Long`을 사용한다 |
| 메시지 길이 | 최대 120자 |
| 자동 이동 체크 주기 | 광장별 `1000ms`마다 이동 대상 펫이 있는지 확인한다 |
| 펫별 자동 이동 대기 시간 | 이동 완료 후 다음 자동 이동까지 `2500~5000ms` 랜덤 대기 |
| 자동 이동 지속 시간 | 목표 거리 기준 `1200~2500ms` |
| 중복 입장 | 유저는 동시에 하나의 광장에만 소속된다. 새 광장 입장 시 기존 광장에서는 서버가 자동 퇴장 처리한다 |
| 업데이트 순서 | `roomRevision`이 높은 스냅샷만 최신 상태로 인정한다 |
| 빈 광장 처리 | 마지막 참가자가 정상 퇴장해 0명이 되면 광장을 삭제한다 |
| 비정상 연결 종료 | WebSocket 끊김 등 비정상 종료는 `30초` 재접속 유예 후 퇴장 처리한다 |

## 공통 규칙

| 항목 | 내용 |
|---|---|
| Base URL | 백엔드 환경 기준. 예: `http://15.164.49.236:8080` |
| 인증 | 모든 API는 `Authorization: Bearer {accessToken}` 필요 |
| Content-Type | `application/json` |
| 광장 시간 형식 | epoch millisecond. 예: `1778509800000` |
| 공통 에러 Response | `{ "code": "ERROR_CODE", "message": "사용자 표시 메시지" }` |

### 시간 형식 예외

기존 친구/방 API 문서의 일반 시간 값은 ISO-8601 문자열을 기준으로 한다.

다만 광장 시스템은 캐릭터 이동 보간과 서버 시간 동기화가 중요하므로, 광장 계약 안의 `joinedAtMillis`, `sentAtMillis`, `startedAtMillis`, `positionUpdatedAtMillis`, `serverNowMillis`는 모두 epoch millisecond를 사용한다.

## API 목록

| 기능 | 사용자 | Method | URL | param | 설명 | Response | 비고 |
|---|---|---|---|---|---|---|---|
| 랜덤 광장 입장 | 로그인 사용자 | POST | `/plazas/random/join` | 없음 | 정원이 남은 랜덤 광장에 입장한다. 없으면 새 광장을 생성한다. | `plaza` | 기존 입장 광장이 있으면 자동 퇴장 |
| 코드 광장 입장 | 로그인 사용자 | POST | `/plazas/code/join` | Body: `code` | 광장 코드로 특정 광장에 입장한다. | `plaza` | 정원 초과 시 `PLAZA_FULL` |
| 현재 광장 조회 | 로그인 사용자 | GET | `/plazas/me/active` | 없음 | 현재 로그인 유저가 입장 중인 광장 스냅샷을 조회한다. | `{ "plaza": PlazaRoom? }` | 앱 복귀/재접속 복구용. 없으면 `{ "plaza": null }` |
| 광장 스냅샷 조회 | 로그인 사용자 | GET | `/plazas/{plazaId}` | Path: `plazaId` | 특정 광장의 최신 스냅샷을 조회한다. | `plaza` | WebSocket 임시 대체 시 `2000ms` polling |
| 광장 퇴장 | 입장 사용자 | POST | `/plazas/{plazaId}/leave` | Path: `plazaId` | 광장에서 퇴장한다. | `204 No Content` | 퇴장 후 다른 참가자에게 스냅샷 push |
| 광장 채팅 보내기 | 입장 사용자 | POST | `/plazas/{plazaId}/messages` | Body: `text` | 광장 채팅 메시지를 보낸다. | `message`, `roomRevision`, `serverTime` | 메시지 최대 120자 |
| 광장 상호작용 요청 | 입장 사용자 | POST | `/plazas/{plazaId}/interactions` | Body: `type`, `targetUserId?` | 펫 상호작용을 요청한다. 서버가 가능 여부와 위치를 결정한다. | `interaction`, `roomRevision`, `serverTime` | `GREET`, `PLAY`, `REST`, `FOLLOW` |
| 광장 실시간 연결 | 입장 사용자 | WebSocket | `/ws/plazas/{plazaId}` | Header: Authorization | 스냅샷, 채팅, 상호작용, 입퇴장을 실시간 수신한다. | server event | MVP는 delta 이벤트 대신 전체 `PLAZA_SNAPSHOT` broadcast |

## 확정 플로우

### 1. 랜덤 광장 입장

```http
POST /plazas/random/join
Authorization: Bearer {accessToken}
Content-Type: application/json
```

Request body는 비워도 된다.

서버는 인증 토큰으로 현재 유저를 식별하고, 서버에 저장된 유저 정보와 펫 정보를 사용해 참가자를 만든다.

```json
{}
```

Response:

```json
{
  "plaza": {
    "plazaId": "plaza_001",
    "plazaCode": "PZ8K21",
    "displayPlazaCode": "PZ-8K21",
    "participants": [
      {
        "userId": "user_me",
        "nickname": "나의루파",
        "pet": {
          "petId": "pet_me",
          "ownerUserId": "user_me",
          "name": "루파",
          "characterAssetKey": "room/characters/lupa_default",
          "appearance": {
            "headSizeScale": 1.0,
            "bodySizeScale": 1.0,
            "eyeSizeScale": 1.0,
            "noseSizeScale": 1.0,
            "mouthSizeScale": 1.0
          },
          "status": {
            "satiety": 80,
            "vitality": 75,
            "isEgg": false
          },
          "personality": "ACTIVE",
          "equippedItemIds": []
        },
        "joinedAtMillis": 1778509800000,
        "position": {
          "x": 0.42,
          "y": 0.68
        },
        "movement": null,
        "positionUpdatedAtMillis": 1778509800000
      }
    ],
    "messages": [],
    "interactions": [],
    "maxParticipants": 4,
    "joinedAtMillis": 1778509800000,
    "roomRevision": 1,
    "serverTime": {
      "serverNowMillis": 1778509800000
    },
    "isServerAuthoritative": true
  }
}
```

### 2. 광장 코드로 입장

프론트 입력값은 `PZ-8K21`, `PZ8K21`, `8K21` 모두 허용할 수 있다.  
서버 저장값은 하이픈 없는 대문자 코드로 정규화한다.

새 광장 입장은 성공한 경우에만 기존 광장을 자동 퇴장 처리한다.

- 랜덤 입장 또는 코드 입장 성공: 기존 광장에서 퇴장 후 새 광장에 입장
- `INVALID_CODE`, `PLAZA_NOT_FOUND`, `PLAZA_FULL` 등 입장 실패: 기존 광장 유지
- 서버는 기존 광장 퇴장과 새 광장 입장을 하나의 트랜잭션으로 처리한다.

```http
POST /plazas/code/join
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "code": "PZ8K21"
}
```

성공 응답은 랜덤 입장과 동일한 `plaza` 구조를 반환한다.

정원이 가득 찬 경우:

```http
409 Conflict
```

```json
{
  "code": "PLAZA_FULL",
  "message": "광장이 가득 찼어요."
}
```

### 3. 현재 광장 조회

앱 복귀, 재접속 복구, WebSocket 재연결 전에 현재 로그인 유저가 광장에 들어가 있는지 확인한다.

```http
GET /plazas/me/active
Authorization: Bearer {accessToken}
```

입장 중인 광장이 있으면:

```json
{
  "plaza": {
    "plazaId": "plaza_001",
    "plazaCode": "PZ8K21",
    "displayPlazaCode": "PZ-8K21",
    "participants": [],
    "messages": [],
    "interactions": [],
    "maxParticipants": 4,
    "joinedAtMillis": 1778509800000,
    "roomRevision": 3,
    "serverTime": {
      "serverNowMillis": 1778509821000
    },
    "isServerAuthoritative": true
  }
}
```

입장 중인 광장이 없으면:

```json
{
  "plaza": null
}
```

두 경우 모두 `200 OK`를 사용한다.

### 4. 광장 채팅 보내기

```http
POST /plazas/{plazaId}/messages
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "text": "안녕! 여기 코드 공유해도 돼?"
}
```

```json
{
  "message": {
    "id": "plaza_message_001",
    "plazaId": "plaza_001",
    "senderUserId": "user_me",
    "senderNickname": "나의루파",
    "text": "안녕! 여기 코드 공유해도 돼?",
    "sentAtMillis": 1778509810000
  },
  "roomRevision": 2,
  "serverTime": {
    "serverNowMillis": 1778509810000
  }
}
```

채팅 성공 후 서버는 같은 광장에 있는 모든 접속자에게 최신 `PLAZA_SNAPSHOT` 전체 스냅샷을 보낸다.

`CHAT_MESSAGE` delta 이벤트는 후순위 확장으로 남긴다.

### 5. 광장 상호작용 요청

상호작용은 프론트가 최종 위치를 확정하지 않는다.  
프론트는 어떤 타입의 상호작용을 하고 싶은지 요청하고, 서버가 현재 위치/쿨다운/대상 상태를 보고 최종 이벤트를 만든다.

```http
POST /plazas/{plazaId}/interactions
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "type": "FOLLOW",
  "targetUserId": "user_friend"
}
```

```json
{
  "interaction": {
    "id": "plaza_interaction_001",
    "plazaId": "plaza_001",
    "type": "FOLLOW",
    "actorUserId": "user_me",
    "targetUserId": "user_friend",
    "textByUserId": {
      "user_me": "같이 가자!",
      "user_friend": "좋아!"
    },
    "startedAtMillis": 1778509820000,
    "durationMillis": 3100,
    "movementTargetByUserId": {
      "user_me": {
        "x": 0.58,
        "y": 0.62
      },
      "user_friend": {
        "x": 0.48,
        "y": 0.68
      }
    },
    "facingTargetByUserId": {
      "user_me": {
        "x": 0.48,
        "y": 0.68
      },
      "user_friend": {
        "x": 0.58,
        "y": 0.62
      }
    },
    "animationByUserId": {},
    "metadata": {}
  },
  "roomRevision": 3,
  "serverTime": {
    "serverNowMillis": 1778509820000
  }
}
```

상호작용 성공 후 서버는 같은 광장에 있는 모든 접속자에게 최신 `PLAZA_SNAPSHOT` 전체 스냅샷을 보낸다.

`INTERACTION_STARTED` delta 이벤트는 후순위 확장으로 남긴다.

## 실시간 동기화 계약

### WebSocket 연결

```http
GET /ws/plazas/{plazaId}
Authorization: Bearer {accessToken}
```

WebSocket 연결이 성공하면 서버는 즉시 `PLAZA_SNAPSHOT` 이벤트를 1회 전송한다.

MVP 필수 구현은 전체 스냅샷 방식이다.

- 연결 직후 `PLAZA_SNAPSHOT` 1회 전송
- 참가자 입장/퇴장, 채팅, 이동, 상호작용 발생 시 최신 `PLAZA_SNAPSHOT` broadcast
- `CHAT_MESSAGE`, `MOVEMENT_UPDATED`, `INTERACTION_STARTED` 같은 delta 이벤트는 후순위 예약
- WebSocket 구현이 늦을 경우 프론트는 `GET /plazas/{plazaId}`를 `2000ms` 간격으로 polling한다.

```json
{
  "type": "PLAZA_SNAPSHOT",
  "roomRevision": 3,
  "serverTime": {
    "serverNowMillis": 1778509821000
  },
  "plaza": {
    "plazaId": "plaza_001",
    "plazaCode": "PZ8K21",
    "displayPlazaCode": "PZ-8K21",
    "participants": [],
    "messages": [],
    "interactions": [],
    "maxParticipants": 4,
    "joinedAtMillis": 1778509800000,
    "roomRevision": 3,
    "serverTime": {
      "serverNowMillis": 1778509821000
    },
    "isServerAuthoritative": true
  }
}
```

### 서버 이벤트 종류

| type | 설명 | payload |
|---|---|---|
| `PLAZA_SNAPSHOT` | 광장 전체 최신 상태 | `plaza`, `roomRevision`, `serverTime` |
| `ERROR` | 실시간 처리 에러 | `code`, `message` |
| `PARTICIPANT_JOINED` | 참가자 입장 | 후순위 delta 이벤트 |
| `PARTICIPANT_LEFT` | 참가자 퇴장 | 후순위 delta 이벤트 |
| `CHAT_MESSAGE` | 채팅 메시지 추가 | 후순위 delta 이벤트 |
| `MOVEMENT_UPDATED` | 참가자 위치/이동 변경 | 후순위 delta 이벤트 |
| `INTERACTION_STARTED` | 상호작용 시작 | 후순위 delta 이벤트 |

MVP 필수 구현 이벤트는 `PLAZA_SNAPSHOT`과 `ERROR`다.  
프론트는 최신 `roomRevision` 기준으로 전체 상태를 갱신한다.

## 데이터 모델

### PlazaRoom

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `plazaId` | `String` | O | 광장 ID |
| `plazaCode` | `String` | O | 공유용 광장 코드. 예: `PZ8K21` |
| `displayPlazaCode` | `String` | O | 화면 표시용 광장 코드. 예: `PZ-8K21` |
| `participants` | `PlazaParticipant[]` | O | 현재 참가자 목록. 최대 4명 |
| `messages` | `PlazaChatMessage[]` | O | 최근 메시지 목록. 최신 50개 |
| `interactions` | `PlazaInteractionEvent[]` | O | 진행 중이거나 최근 완료 전인 상호작용 목록 |
| `maxParticipants` | `Int` | O | 기본값 4 |
| `joinedAtMillis` | `Long` | O | 현재 유저가 이 광장에 입장한 시각 |
| `roomRevision` | `Long` | O | 광장 상태 revision |
| `serverTime` | `PlazaServerTime` | O | 서버 시간 스냅샷 |
| `isServerAuthoritative` | `Boolean` | O | 실제 API 응답에서는 항상 `true` |

### PlazaParticipant

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `userId` | `String` | O | 참가자 유저 ID |
| `nickname` | `String` | O | 화면 표시 이름 |
| `pet` | `PlazaPetSnapshot` | O | 광장에 표시할 펫 스냅샷 |
| `joinedAtMillis` | `Long` | O | 참가자가 광장에 입장한 시각 |
| `position` | `PlazaPosition` | O | 서버가 계산한 현재 위치. `movement`가 있어도 반드시 포함 |
| `movement` | `PlazaMovementCommand?` | O | 진행 중인 이동 명령. 없으면 `null` |
| `positionUpdatedAtMillis` | `Long` | O | `position` 또는 `movement`가 마지막으로 갱신된 시각 |

`isMe`는 서버 응답에 포함하지 않아도 된다.  
프론트는 로그인 유저 ID와 `participant.userId`를 비교해 계산한다.

### PlazaPetSnapshot

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `petId` | `String` | O | 펫 ID |
| `ownerUserId` | `String` | O | 펫 주인 유저 ID |
| `name` | `String` | O | 펫 이름 |
| `characterAssetKey` | `String` | O | 캐릭터 에셋 키 |
| `appearance` | `PetAppearance` | O | 외형 배율 값 |
| `status` | `PetStatus` | O | `satiety`, `vitality`, `isEgg` |
| `personality` | `PetPersonality` | O | `ACTIVE`, `CALM`, `LAZY` |
| `equippedItemIds` | `String[]` | O | 착용 중인 아이템 ID 목록. 없으면 `[]` |

### PlazaPosition

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `x` | `Float` | O | 광장 화면 기준 정규화 x 좌표 |
| `y` | `Float` | O | 광장 화면 기준 정규화 y 좌표 |

서버는 위치를 다음 범위로 clamp해서 내려준다.

| 좌표 | 최소 | 최대 |
|---|---:|---:|
| `x` | `0.14` | `0.86` |
| `y` | `0.47` | `0.88` |

### PlazaMovementCommand

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `from` | `PlazaPosition` | O | 이동 시작 위치 |
| `to` | `PlazaPosition` | O | 이동 목표 위치 |
| `startedAtMillis` | `Long` | O | 서버 기준 이동 시작 시각 |
| `durationMillis` | `Long` | O | 이동 지속 시간. 허용 범위 `250~6000` |

프론트는 `serverTime.serverNowMillis`를 기준으로 `from -> to`를 보간한다.

### 이동 종료 처리

서버는 `startedAtMillis + durationMillis` 이후 다음 광장 tick에서 완료된 이동을 정리한다.

완료 처리 규칙:

```text
position = movement.to
movement = null
positionUpdatedAtMillis = movement.startedAtMillis + movement.durationMillis
roomRevision 증가
```

서버가 정확히 millisecond 단위로 종료 이벤트를 보낼 필요는 없다.  
다음 `1000ms` 광장 tick에서 이미 끝난 `movement`를 정리하고 최신 `PLAZA_SNAPSHOT`을 broadcast하면 된다.

### PlazaServerTime

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `serverNowMillis` | `Long` | O | 응답 생성 시점의 서버 현재 시각 |

프론트 내부 모델에는 `clientReceivedAtMillis`가 있지만, 이 값은 백엔드가 내려주지 않는다.  
프론트 API mapper가 응답을 받은 순간의 로컬 시간을 넣어 계산한다.

### PlazaChatMessage

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `id` | `String` | O | 메시지 ID |
| `plazaId` | `String` | O | 광장 ID |
| `senderUserId` | `String` | O | 보낸 유저 ID |
| `senderNickname` | `String` | O | 보낸 유저 닉네임 |
| `text` | `String` | O | 메시지 내용. 최대 120자 |
| `sentAtMillis` | `Long` | O | 서버 기준 발송 시각 |

### 메시지 보관 정책

광장 스냅샷의 `messages`는 최신 50개만 포함한다.

- 정렬: 오래된 메시지 -> 최신 메시지
- 새 메시지: 배열 마지막에 추가
- 50개 초과: 오래된 메시지부터 제외

### PlazaInteractionEvent

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `id` | `String` | O | 상호작용 이벤트 ID |
| `plazaId` | `String` | O | 광장 ID |
| `type` | `PlazaInteractionType` | O | `GREET`, `PLAY`, `REST`, `FOLLOW` |
| `actorUserId` | `String` | O | 상호작용을 시작한 유저 |
| `targetUserId` | `String?` | O | 대상 유저. `GREET`, `PLAY`, `FOLLOW`는 필수. `REST`만 `null` 허용 |
| `textByUserId` | `Map<String, String>` | O | 유저별 말풍선 텍스트. 없으면 `{}` |
| `startedAtMillis` | `Long` | O | 서버 기준 시작 시각 |
| `durationMillis` | `Long` | O | 지속 시간 |
| `movementTargetByUserId` | `Map<String, PlazaPosition>` | O | 유저별 상호작용 목표 위치 |
| `facingTargetByUserId` | `Map<String, PlazaPosition>` | O | 유저별 바라볼 위치. 바라볼 위치가 없으면 `{}` |
| `animationByUserId` | `Map<String, String>` | O | 유저별 강제 애니메이션. 없으면 `{}` |
| `metadata` | `Map<String, String>` | O | 확장용 메타데이터. 없으면 `{}` |

서버 권위 동기화에서는 `movementTargetByUserId`를 서버가 반드시 계산해서 내려준다.  
`facingTargetByUserId`는 유저별로 특정 방향을 보게 해야 하는 상호작용에서는 반드시 채우고, 별도 방향이 필요 없는 경우에는 `{}`로 내려준다.

## 상태값

### PlazaInteractionType

| 값 | 설명 |
|---|---|
| `GREET` | 두 펫이 가까이 이동해 인사한다 |
| `PLAY` | 두 펫이 가까이 이동해 놀이 행동을 한다 |
| `REST` | 한 명 또는 두 명의 펫이 짧은 휴식 행동을 한다 |
| `FOLLOW` | 한 펫이 리더가 되고 다른 펫이 따라가는 위치로 이동한다 |

### 상호작용 targetUserId 규칙

| type | `targetUserId` | 설명 |
|---|---|---|
| `GREET` | 필수 | 대상 펫에게 인사한다 |
| `PLAY` | 필수 | 대상 펫과 놀이 행동을 한다 |
| `FOLLOW` | 필수 | 대상 펫을 따라가거나 함께 이동한다 |
| `REST` | 선택 | 혼자 쉬기면 `null`, 함께 쉬기면 대상 유저 ID |

### CharacterAnimation

`animationByUserId`는 현재 프론트 임시 스프라이트 행 이름을 그대로 받을 수 있다.

| 값 | 설명 |
|---|---|
| `Row0` | 기본 행 |
| `Row1` | 임시 행 1 |
| `Row2` | 임시 행 2 |
| `Row3` | 임시 행 3. 현재 `REST` idle에 사용 |

MVP에서는 서버가 `animationByUserId`를 비워도 된다.  
프론트는 상호작용 타입별 기본 애니메이션을 사용한다.

## roomRevision 규칙

서버는 프론트 화면에 반영되어야 하는 모든 변경마다 `roomRevision`을 증가시킨다.

증가해야 하는 경우:

- 참가자 입장
- 참가자 퇴장
- 채팅 메시지 추가
- 참가자 위치 변경
- 참가자 이동 명령 변경
- 상호작용 시작
- 상호작용 종료 또는 만료 반영

프론트는 같은 `plazaId`에서 `roomRevision`이 현재 값보다 낮거나 같은 스냅샷은 무시할 수 있다.  
따라서 서버는 같은 revision으로 서로 다른 화면 상태를 보내지 않아야 한다.

## 입장/퇴장 상태 처리

### 새 광장 입장

유저가 이미 다른 광장에 들어가 있는 상태에서 랜덤 입장 또는 코드 입장을 요청할 수 있다.

처리 규칙:

- 새 광장 입장이 성공하면 기존 광장에서 자동 퇴장 처리한다.
- 새 광장 입장이 실패하면 기존 광장 상태를 유지한다.
- 실패 예: `INVALID_CODE`, `PLAZA_NOT_FOUND`, `PLAZA_FULL`, `UNAUTHORIZED`
- 기존 광장 퇴장과 새 광장 입장을 하나의 트랜잭션으로 처리한다.

### 광장 삭제

마지막 참가자가 정상 퇴장해 광장 참가자가 0명이 되면 서버는 해당 광장을 즉시 삭제한다.

삭제된 광장의 코드로 다시 입장하려고 하면 `PLAZA_NOT_FOUND`를 반환한다.

### 비정상 연결 종료

WebSocket 끊김, 앱 강제 종료, 네트워크 전환처럼 명시적 퇴장이 아닌 경우에는 `30초` 재접속 유예 시간을 둔다.

처리 규칙:

- 끊김 감지 시 참가자를 즉시 삭제하지 않는다.
- `30초` 안에 같은 유저가 재접속하면 기존 광장 참가자로 복구한다.
- `30초`가 지나도 재접속하지 않으면 퇴장 처리한다.
- 퇴장 처리 후 참가자가 0명이면 광장을 삭제한다.

## 자동 이동 정책

서버는 광장 안의 펫을 매 프레임 이동시키지 않는다.  
광장별로 일정 주기마다 이동 대상 펫만 골라 `PlazaMovementCommand`를 생성한다.

### 자동 이동 주기

| 항목 | 값 | 설명 |
|---|---:|---|
| 광장 체크 주기 | `1000ms` | 광장별로 이동 대상 펫이 있는지 확인하는 주기 |
| 펫별 이동 대기 시간 | `2500~5000ms` | 이동 완료 후 다음 자동 이동까지의 랜덤 대기 시간 |
| 이동 지속 시간 | `1200~2500ms` | 목표 위치까지 걸어가는 데 걸리는 시간 |
| 후보 재시도 횟수 | `8회` | 겹치지 않는 목표 위치를 찾기 위한 최대 재시도 횟수 |
| 최소 이동 거리 | `0.04` | 너무 짧은 이동은 무시 |
| 다른 펫과의 최소 거리 | `0.09` | 목표 위치가 다른 펫과 너무 겹치지 않도록 하는 기준 |

상호작용 중인 펫은 자동 이동 대상에서 제외한다.

### 가까운 랜덤 목표 좌표 선정

목표 좌표는 광장 전체에서 완전 랜덤으로 고르지 않는다.  
현재 위치 주변의 작은 타원 범위 안에서 후보를 뽑아 자연스럽게 산책하는 느낌을 만든다.

자동 이동 반경:

| 방향 | 최소 | 최대 |
|---|---:|---:|
| `x` 방향 | `0.08` | `0.18` |
| `y` 방향 | `0.04` | `0.10` |

선정 절차:

1. 현재 위치를 `current`로 둔다.
2. `0~2π` 범위에서 랜덤 방향 `angle`을 선택한다.
3. `x` 이동 반경은 `0.08~0.18`, `y` 이동 반경은 `0.04~0.10`에서 랜덤 선택한다.
4. 후보 좌표를 계산한다.
5. 후보 좌표를 광장 이동 가능 범위로 clamp한다.
6. 현재 위치와의 거리가 `0.04` 이상인지 확인한다.
7. 다른 펫 위치와의 거리가 `0.09` 이상인지 확인한다.
8. 조건을 만족하면 목표 좌표로 확정한다.
9. 최대 `8회` 실패하면 억지로 이동시키지 않고 현재 위치를 유지하거나 다음 체크로 넘긴다.

계산 예시:

```text
candidate.x = current.x + cos(angle) * random(0.08, 0.18)
candidate.y = current.y + sin(angle) * random(0.04, 0.10)

candidate.x = clamp(candidate.x, 0.14, 0.86)
candidate.y = clamp(candidate.y, 0.47, 0.88)
```

서버 pseudo code:

```kotlin
fun pickNearbyTarget(
    current: PlazaPosition,
    otherPositions: List<PlazaPosition>
): PlazaPosition? {
    repeat(8) {
        val angle = random(0.0, Math.PI * 2)
        val radiusX = random(0.08, 0.18)
        val radiusY = random(0.04, 0.10)

        val candidate = PlazaPosition(
            x = (current.x + cos(angle) * radiusX).coerceIn(0.14, 0.86),
            y = (current.y + sin(angle) * radiusY).coerceIn(0.47, 0.88)
        )

        val movedEnough = distance(current, candidate) >= 0.04
        val farEnough = otherPositions.all { other ->
            distance(candidate, other) >= 0.09
        }

        if (movedEnough && farEnough) {
            return candidate
        }
    }

    return null
}
```

`null`이면 이번 체크에서는 이동 명령을 만들지 않는다.

### 이동 명령 생성

목표 좌표가 확정되면 서버는 다음 형태의 `movement`를 생성한다.

```json
{
  "from": {
    "x": 0.42,
    "y": 0.68
  },
  "to": {
    "x": 0.55,
    "y": 0.61
  },
  "startedAtMillis": 1778509820000,
  "durationMillis": 1800
}
```

`durationMillis`는 이동 거리에 따라 조절한다.

| 이동 거리 | duration |
|---|---:|
| 짧은 이동 | `1200ms` |
| 중간 이동 | `1700ms` |
| 긴 이동 | `2300ms` |

서버는 이동 명령을 생성할 때 `roomRevision`을 증가시키고, 같은 광장 참가자에게 최신 `PLAZA_SNAPSHOT`을 broadcast한다.

## 서버 부하 방지 확정안

광장은 서버가 권위(authoritative)를 갖되, 매 프레임 위치를 보내지 않는다.

확정 방식:

1. 서버는 참가자별 다음 이동 명령을 일정 간격으로만 생성한다.
2. 서버는 `from`, `to`, `startedAtMillis`, `durationMillis`만 내려준다.
3. 클라이언트는 이 명령을 화면에서 부드럽게 보간한다.
4. 서버는 위치 변경/채팅/상호작용/입퇴장 같은 상태 변화 때만 broadcast한다.
5. 광장별 최대 인원이 4명이므로, 광장이 많아져도 방 단위로 작은 상태만 관리한다.

피해야 할 방식:

- 서버가 60fps로 모든 참가자 위치를 계속 broadcast
- 클라이언트가 임의로 결정한 다른 유저 위치를 서버가 검증 없이 그대로 신뢰
- `roomRevision` 증가 없이 서로 다른 스냅샷 전송

## 에러 코드

| code | HTTP | 설명 |
|---|---:|---|
| `EMPTY_CODE` | `400` | 광장 코드가 비어 있음 |
| `INVALID_CODE` | `400` | 광장 코드 형식이 올바르지 않음 |
| `PLAZA_NOT_FOUND` | `404` | 해당 광장을 찾을 수 없음 |
| `PLAZA_FULL` | `409` | 광장 정원이 가득 참 |
| `NOT_IN_PLAZA` | `403` | 해당 광장에 입장 중이 아님 |
| `EMPTY_MESSAGE` | `400` | 메시지가 비어 있음 |
| `MESSAGE_TOO_LONG` | `400` | 메시지가 최대 길이 120자를 초과함 |
| `INTERACTION_TARGET_NOT_FOUND` | `404` | 상호작용 대상 참가자를 찾을 수 없음 |
| `INTERACTION_NOT_ALLOWED` | `409` | 거리, 쿨다운, 상태 등으로 상호작용할 수 없음 |
| `UNAUTHORIZED` | `401` | 인증 토큰 없음 또는 만료 |
| `UNKNOWN` | `500` | 알 수 없는 서버 오류 |

## 프론트 연동 메모

- 실제 API 응답의 `PlazaRoom.isServerAuthoritative`는 항상 `true`로 내려온다.
- 서버 응답의 `serverTime.serverNowMillis`를 받을 때, 프론트 mapper가 `clientReceivedAtMillis = System.currentTimeMillis()`를 채운다.
- 서버는 `participants[].position`을 항상 내려준다. `movement`가 있어도 `position`은 생략하지 않는다.
- `participants[].movement`는 진행 중인 이동이 없으면 `null`이다.
- `messages`는 최신 50개를 오래된 순서에서 최신 순서로 정렬해 내려준다.
- `interactions`는 진행 중이거나 아직 화면에 표시되어야 하는 최근 이벤트만 내려준다.
