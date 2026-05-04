# 루파루파 API 관리 테이블

버전: `0.1`  
상태: MVP 백엔드 연동용 초안  
기준 문서: `docs/api-contract.md`

## 공통 규칙

| 항목 | 내용 |
|---|---|
| Base URL | 미정. 예시: `https://api.lupalupa.com` |
| 인증 | 보호 API는 `Authorization: Bearer {accessToken}` 사용 |
| Content-Type | `application/json` |
| 시간 형식 | ISO-8601 문자열. 예: `2026-05-04T18:30:00+09:00` |
| 공통 에러 Response | `{ "code": "ERROR_CODE", "message": "사용자 표시 메시지" }` |

## API 목록

| 기능 | 카테고리 | 사용자 | Method | URL | param | 설명 | Response | 비고 |
|---|---|---|---|---|---|---|---|---|
| 카카오 로그인 | 인증 | 비로그인 사용자 | POST | `/auth/kakao` | Body: `kakaoAccessToken` | 카카오 SDK에서 받은 토큰으로 앱 자체 토큰을 발급한다. | `accessToken`, `refreshToken`, `user{userId, nickname, friendCode, avatarAssetKey}` | 최초 로그인 시 서버에서 유저와 친구 코드를 생성한다. |
| 토큰 갱신 | 인증 | 로그인 사용자 | POST | `/auth/refresh` | Body: `refreshToken` | 만료된 access token을 갱신한다. | `accessToken`, `refreshToken` | 에러: `INVALID_REFRESH_TOKEN`, `EXPIRED_REFRESH_TOKEN` |
| 내 정보 조회 | 유저 | 로그인 사용자 | GET | `/users/me` | 없음 | 현재 로그인한 유저 정보를 조회한다. | `userId`, `nickname`, `friendCode`, `avatarAssetKey`, `currencyAmount` | `userId`는 내부 식별자, `friendCode`는 친구 추가용 공개 코드다. |
| 내 친구 코드 조회 | 유저 | 로그인 사용자 | GET | `/users/me/friend-code` | 없음 | 내 친구 코드를 조회한다. | `friendCode`, `displayFriendCode` | 화면에는 `displayFriendCode`, API 요청에는 `friendCode` 사용을 권장한다. |
| 내 펫 정보 조회 | 펫 | 로그인 사용자 | GET | `/pets/me` | 없음 | 내 펫의 외형, 상태, 성격, 치장 정보를 조회한다. | `pet{petId, ownerUserId, name, appearance, status, personality, equippedItemIds}` | 외형 크기값은 최초 생성 시 서버 랜덤 생성. |
| 내 펫 상태 업데이트 | 펫 | 로그인 사용자 | PATCH | `/pets/me/status` | Body: `hunger`, `fatigue`, `isEgg`, `action`, `anchor` | 먹이, 휴식, 놀이, 알 부화 등으로 바뀐 펫 상태를 저장한다. | `pet{status, updatedAt}` | `hunger`, `fatigue`는 `0~100`. 운영 단계에서는 서버 계산 방식 권장. |
| 내 펫 치장 변경 | 펫 | 로그인 사용자 | PUT | `/pets/me/equipment` | Body: `equippedItemIds` | 펫이 착용 중인 치장 아이템 목록을 변경한다. | `equippedItemIds`, `updatedAt` | 착용 아이템이 없으면 `[]`. 에러: `ITEM_NOT_OWNED`, `ITEM_NOT_EQUIPPABLE` |
| 친구 신청 보내기 | 친구 | 로그인 사용자 | POST | `/friends/requests` | Body: `friendCode` | 상대방 친구 코드를 입력해 친구 신청을 보낸다. | `request{id, fromUser, toUser, status, createdAt}` | 에러: `SELF_CODE`, `USER_NOT_FOUND`, `ALREADY_FRIENDS`, `REQUEST_ALREADY_SENT` |
| 받은 친구 신청 목록 | 친구 | 로그인 사용자 | GET | `/friends/requests/received` | 없음 | 나에게 온 친구 신청 목록을 조회한다. | `requests[]` | 친구요청목록 탭에서 사용한다. |
| 보낸 친구 신청 목록 | 친구 | 로그인 사용자 | GET | `/friends/requests/sent` | 없음 | 내가 보낸 친구 신청 목록을 조회한다. | `requests[]` | 보낸 요청 취소 UI가 생기면 사용한다. |
| 친구 신청 수락 | 친구 | 요청 수신자 | POST | `/friends/requests/{requestId}/accept` | Path: `requestId` | 받은 친구 신청을 수락하고 친구 관계를 생성한다. | `request{status}`, `friendship{id, friend, createdAt}` | 에러: `REQUEST_NOT_FOUND`, `REQUEST_ALREADY_RESPONDED`, `NOT_REQUEST_RECEIVER` |
| 친구 신청 거절 | 친구 | 요청 수신자 | POST | `/friends/requests/{requestId}/reject` | Path: `requestId` | 받은 친구 신청을 거절한다. | `request{id, status, respondedAt}` | 거절 후 재신청 가능 여부는 백엔드와 합의 필요. |
| 보낸 친구 신청 취소 | 친구 | 요청 발신자 | POST | `/friends/requests/{requestId}/cancel` | Path: `requestId` | 내가 보낸 친구 신청을 취소한다. | `request{id, status, respondedAt}` | 에러: `NOT_REQUEST_SENDER` |
| 친구 목록 조회 | 친구 | 로그인 사용자 | GET | `/friends` | 없음 | 수락된 친구 목록을 조회한다. | `friends[]` | 친구 화면의 기본 목록 데이터다. |
| 친구 삭제 | 친구 | 로그인 사용자 | DELETE | `/friends/{friendUserId}` | Path: `friendUserId` | 친구 관계를 삭제한다. | `204 No Content` | 에러: `FRIEND_NOT_FOUND` |
| 친구 집 방문 정보 조회 | 친구 집 | 친구 관계 사용자 | GET | `/friends/{friendUserId}/home` | Path: `friendUserId` | 친구의 집 화면을 열 때 필요한 방 레이아웃과 펫 정보를 조회한다. | `owner`, `roomLayout`, `pet`, `visitedAt` | 친구가 아닌 유저는 `403` 또는 `404`. |
| 친구 메시지 목록 조회 | 친구 메시지 | 친구 관계 사용자 | GET | `/friends/{friendUserId}/messages` | Path: `friendUserId`<br>Query: `limit`, `before` | 친구와 주고받은 짧은 메시지 목록을 조회한다. | `messages[]`, `nextCursor` | MVP에서는 실시간 채팅 대신 REST 목록으로 시작한다. |
| 친구 메시지 보내기 | 친구 메시지 | 친구 관계 사용자 | POST | `/friends/{friendUserId}/messages` | Path: `friendUserId`<br>Body: `content` | 친구에게 짧은 메시지를 보낸다. | `message{id, sender, receiver, content, createdAt}` | 권장 최대 길이 200자. 에러: `MESSAGE_TOO_LONG` |
| 방 레이아웃 검증 | 방 | 로그인 사용자 | POST | `/rooms/me/layout/validate` | Body: `localLayoutRevision`, `localLayoutHash`, `localUpdatedAt` | 앱 접속 시 로컬 방 상태와 서버 방 상태가 같은지 검증한다. | `syncStatus`, `serverLayoutRevision`, `serverLayoutHash`, `roomLayout` | `MATCH`, `SERVER_NEWER`, `CLIENT_NEWER`, `CONFLICT` 중 하나를 반환한다. |
| 내 방 레이아웃 조회 | 방 | 로그인 사용자 | GET | `/rooms/me/layout` | 없음 | 서버에 저장된 내 방 레이아웃을 조회한다. | `roomLayout{wallAssetKey, floorAssetKey, placedItems, layoutRevision, layoutHash}` | 검증 실패 또는 강제 새로고침 때 사용한다. |
| 내 방 레이아웃 저장 | 방 | 로그인 사용자 | PUT | `/rooms/me/layout` | Body: `baseLayoutRevision`, `wallAssetKey`, `floorAssetKey`, `placedItems` | 벽, 바닥, 가구 배치 변경 후 현재 레이아웃 전체 snapshot을 저장한다. | `roomLayout{layoutRevision, layoutHash, placedItems, updatedAt}` | 에러: `LAYOUT_CONFLICT`, `ITEM_NOT_OWNED`, `TILE_ALREADY_OCCUPIED` |
| 내 재화 조회 | 재화 | 로그인 사용자 | GET | `/currency/me` | 없음 | 현재 보유 재화를 조회한다. | `amount` | 상점, 미니게임 보상 UI에서 사용한다. |
| 재화 지급 | 재화 | 로그인 사용자 | POST | `/currency/earn` | Body: `amount`, `reason`, `idempotencyKey` | 미니게임, 출석 등으로 재화를 지급한다. | `amount`, `transaction{id, type, amount, reason, createdAt}` | 중복 지급 방지를 위해 `idempotencyKey` 사용 권장. |
| 재화 차감 | 재화 | 로그인 사용자 | POST | `/currency/spend` | Body: `amount`, `reason`, `referenceId` | 상점 구매 등으로 재화를 차감한다. | `amount`, `transaction{id, type, amount, reason, createdAt}` | 에러: `INSUFFICIENT_CURRENCY`, `INVALID_AMOUNT` |
| 상점 아이템 목록 | 상점 | 로그인 사용자 | GET | `/shop/items` | 없음 | 구매 가능한 상점 아이템 목록을 조회한다. | `items[]` | 카테고리 예시: `FURNITURE`, `WALLPAPER`, `FLOOR`, `DECORATION`, `PET_ITEM` |
| 상점 아이템 구매 | 상점 | 로그인 사용자 | POST | `/shop/items/{itemId}/purchase` | Path: `itemId` | 특정 상점 아이템을 구매한다. | `purchasedItem`, `currencyAmount` | 재화 차감과 인벤토리 지급은 서버 트랜잭션으로 처리한다. |
| 내 인벤토리 조회 | 인벤토리 | 로그인 사용자 | GET | `/inventory/me` | 없음 | 내가 구매한 아이템 목록을 조회한다. | `items[]` | 방 배치, 펫 치장 변경 시 소유 검증에 사용한다. |
| 미니게임 결과 제출 | 미니게임 | 로그인 사용자 | POST | `/minigames/{gameId}/results` | Path: `gameId`<br>Body: `score`, `durationSeconds`, `clientResultId` | 미니게임 결과를 제출하고 보상을 받는다. | `resultId`, `rewardAmount`, `currencyAmount`, `createdAt` | 보상 계산은 서버에서 수행하는 것을 권장한다. |
| 갤러리 항목 목록 | 갤러리 | 로그인 사용자 | GET | `/gallery/items` | 없음 | 저장된 갤러리 항목 목록을 조회한다. | `items[]` | MVP 초반에는 로컬 저장 우선, 서버 동기화는 후순위 가능. |
| 갤러리 항목 등록 | 갤러리 | 로그인 사용자 | POST | `/gallery/items` | Body: `title`, `imageUrl`, `thumbnailUrl` | 스크린샷 갤러리 메타데이터를 서버에 등록한다. | `item{id, title, imageUrl, thumbnailUrl, createdAt}` | 이미지 파일 업로드 방식은 별도 합의 필요. |

## 상태값 정리

| 구분 | 값 | 설명 |
|---|---|---|
| 친구 요청 상태 | `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELED` | 친구 신청 처리 상태 |
| 친구 관계 상태 | `NONE`, `PENDING_SENT`, `PENDING_RECEIVED`, `ACCEPTED`, `REJECTED`, `CANCELED`, `BLOCKED` | 두 유저 간 관계 상태 |
| 펫 행동 | `IDLE`, `RESTING`, `PLAYING`, `EATING` | 현재 펫 행동 |
| 펫 성격 | `ACTIVE`, `CALM`, `LAZY` | 활발, 차분, 게으름 |
| 방 레이아웃 동기화 | `MATCH`, `SERVER_NEWER`, `CLIENT_NEWER`, `CONFLICT` | 로컬/서버 방 상태 비교 결과 |
| 가구 anchor type | `FLOOR`, `WALL` | 바닥 배치 또는 벽 배치 |
| 가구 anchor mode | `CENTER`, `FRONT_CENTER` | 타일 기준 배치 기준점 |
| 상점 카테고리 | `FURNITURE`, `WALLPAPER`, `FLOOR`, `DECORATION`, `PET_ITEM` | 상점 아이템 분류 |

