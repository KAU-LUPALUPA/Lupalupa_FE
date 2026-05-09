# 루파루파 펫 포만감/활력/휴식 설계

버전: `0.1`  
상태: 데모 테스트용 설계 초안  
기준일: 2026-05-09  
범위: 펫 포만감, 활력, 먹이 행동, 휴식 행동, 추후 밤 수면 확장 지점

## 설계 목표

펫은 시간이 지나면 포만감과 활력이 자연스럽게 감소한다.  
유저는 먹이 행동으로 포만감을 회복시키고, 침대/소파/빈백 같은 휴식 가구에서 쉬게 하여 활력을 회복시킨다.

현재 단계는 데모 테스트가 중요하므로 감소/회복 주기를 초 단위로 둔다.  
다만 실제 서비스 밸런스에서는 분 단위로 바뀔 수 있으므로, 로직은 정책값만 교체할 수 있게 설계한다.

## 용어 정리

| 용어 | 영문 키 | 범위 | 의미 |
|---|---|---:|---|
| 포만감 | `satiety` | `0~100` | 높을수록 배가 부른 상태 |
| 활력 | `vitality` | `0~100` | 높을수록 활동 가능한 상태 |
| 휴식 | `RESTING` | 행동 상태 | 낮잠, 소파/빈백/침대 위 짧은 휴식 |
| 밤 수면 | `SLEEPING` | 추후 행동 상태 | 전등을 끄면 침대로 가서 자는 장시간 수면 |

`satiety`와 `vitality`는 둘 다 `100`이 좋은 상태, `0`이 나쁜 상태로 통일한다.

## 현재 코드와의 관계

현재 코드에는 `PetStatus(satiety, vitality, isEgg)`가 있다.

```kotlin
data class PetStatus(
    val satiety: Int = 80,
    val vitality: Int = 80,
    val isEgg: Boolean = false
)
```

현재 프론트 내부 모델과 API 계약 방향은 같은 이름을 사용한다.

| 필드 | 설계상 의미 | 처리 |
|---|---|---|
| `satiety` | 포만감 | 시간이 지나면 감소, 먹이 행동으로 회복 |
| `vitality` | 활력 | 시간이 지나면 감소, 휴식 행동으로 회복 |
| `isEgg` | 알 상태 여부 | 유지 |

이전 `fatigue` 저장값을 서버나 로컬 저장소에서 가져와야 하는 경우에는 마이그레이션 시 `vitality = 100 - fatigue`로 변환한다.

## 권장 도메인 모델

```kotlin
data class PetCondition(
    val satiety: Int = 80,
    val vitality: Int = 80,
    val isEgg: Boolean = false,
    val updatedAtMillis: Long
)
```

상태 계산은 `updatedAtMillis` 기준으로 한다.  
앱이 꺼져 있는 동안에도 시간이 흐르기 때문에, 매초 상태를 저장하기보다는 마지막 계산 시각에서 현재 시각까지의 경과 시간을 한 번에 반영한다.

## 행동 상태 정의

| 상태 | 의미 | 포만감 | 활력 |
|---|---|---|---|
| `IDLE` | 대기 중 | 자연 감소 | 자연 감소 |
| `WALKING` | 이동 중 | 자연 감소 | 자연 감소 |
| `EATING` | 먹는 중 | 즉시 회복 | 자연 감소 또는 변화 없음 |
| `PLAYING` | 노는 중 | 자연 감소 | 자연 감소. 추후 더 빠른 감소 가능 |
| `RESTING` | 낮잠/휴식 | 자연 감소 또는 약간 느린 감소 | 지속 회복 |
| `SLEEPING` | 밤 수면. 추후 추가 | 매우 느린 감소 | 빠른 지속 회복 |

현재 구현 대상은 `RESTING`이다.  
`SLEEPING`은 전등 끄기, 자동 침대 이동, 밤 수면 연출이 들어갈 때 별도 상태로 추가한다.

## 데모 테스트 정책값

초 단위로 빠르게 변화가 보이도록 설정한다.

| 항목 | 값 |
|---|---:|
| 포만감 자연 감소 | `5초마다 -1` |
| 활력 자연 감소 | `4초마다 -1` |
| 먹이 회복량 | `+20` |
| 휴식 활력 회복 | `2초마다 +3` |
| 휴식 중 포만감 감소 | 기본 감소와 동일. 필요하면 `0.7배`로 완화 |
| 최소값/최대값 | `0~100` |

데모 중 앱을 오래 꺼두었을 때 바로 `0`까지 떨어지는 일이 부담스럽다면, 데모 모드에서는 오프라인 반영 시간을 최대 `120초`로 제한한다.

## 서비스 밸런스용 정책값

추후 실제 서비스에서는 아래 정도로 느리게 바꿀 수 있다.

| 항목 | 값 |
|---|---:|
| 포만감 자연 감소 | `10분마다 -1` |
| 활력 자연 감소 | `12분마다 -1` |
| 먹이 회복량 | `+25` |
| 휴식 활력 회복 | `1분마다 +3` |
| 밤 수면 활력 회복 | `1분마다 +5` |
| 밤 수면 포만감 감소 | 일반 감소의 `0.2배` |

## 정책 모델

```kotlin
data class PetConditionPolicy(
    val satietyDecayIntervalSeconds: Long,
    val satietyDecayAmount: Int,
    val vitalityDecayIntervalSeconds: Long,
    val vitalityDecayAmount: Int,
    val feedRecoveryAmount: Int,
    val restRecoveryIntervalSeconds: Long,
    val restRecoveryAmount: Int,
    val restSatietyDecayMultiplier: Float,
    val maxOfflineApplySeconds: Long? = null
)
```

데모 정책:

```kotlin
val DemoPetConditionPolicy = PetConditionPolicy(
    satietyDecayIntervalSeconds = 5,
    satietyDecayAmount = 1,
    vitalityDecayIntervalSeconds = 4,
    vitalityDecayAmount = 1,
    feedRecoveryAmount = 20,
    restRecoveryIntervalSeconds = 2,
    restRecoveryAmount = 3,
    restSatietyDecayMultiplier = 1.0f,
    maxOfflineApplySeconds = 120
)
```

운영 정책:

```kotlin
val ProductionPetConditionPolicy = PetConditionPolicy(
    satietyDecayIntervalSeconds = 600,
    satietyDecayAmount = 1,
    vitalityDecayIntervalSeconds = 720,
    vitalityDecayAmount = 1,
    feedRecoveryAmount = 25,
    restRecoveryIntervalSeconds = 60,
    restRecoveryAmount = 3,
    restSatietyDecayMultiplier = 0.7f
)
```

## 상태 계산 로직

상태 계산은 순수 함수로 분리한다.

```kotlin
fun tickCondition(
    condition: PetCondition,
    action: PetAction,
    nowMillis: Long,
    policy: PetConditionPolicy
): PetCondition
```

기본 흐름:

1. `nowMillis - condition.updatedAtMillis`로 경과 시간을 구한다.
2. 데모 모드의 `maxOfflineApplySeconds`가 있으면 경과 시간을 제한한다.
3. 경과 시간에 맞춰 포만감 자연 감소를 반영한다.
4. `RESTING`이 아니면 활력 자연 감소를 반영한다.
5. `RESTING`이면 활력 지속 회복을 반영한다.
6. 모든 값은 `0~100`으로 보정한다.
7. 계산 후 `updatedAtMillis = nowMillis`로 갱신한다.

## 먹이 행동

먹이 행동은 포만감을 즉시 회복한다.

조건:

| 조건 | 처리 |
|---|---|
| 알 상태 `isEgg = true` | 먹이 행동 불가 또는 별도 알 상호작용 |
| 현재 `RESTING` | 휴식 종료 후 먹기 또는 먹기 거부. MVP에서는 휴식 종료 후 먹기 권장 |
| `satiety = 100` | 먹이 소모 없이 "이미 배불러요" 처리 가능 |

흐름:

1. 현재 시각 기준으로 먼저 `tickCondition`을 실행한다.
2. `satiety += feedRecoveryAmount`를 적용한다.
3. `PetAction.EATING`으로 변경한다.
4. 짧은 애니메이션 후 `IDLE`로 돌아간다.

## 휴식 행동

휴식은 활력을 지속 회복하는 행동이다.  
침대뿐 아니라 추후 소파, 빈백 등 휴식 가능한 가구에서 공통으로 사용한다.

조건:

| 조건 | 처리 |
|---|---|
| 알 상태 `isEgg = true` | 휴식 행동 불가 |
| 이미 `RESTING` | 중복 실행 무시 |
| `vitality = 100` | 휴식은 가능하지만 회복 효과는 없음. MVP에서는 "충분히 활기차요" 처리 가능 |
| 먹는 중 `EATING` | 먹기 완료 후 휴식 |
| 이동 중 `WALKING` | 목적지 도착 후 휴식 |

흐름:

1. 유저가 침대/소파/빈백 등 휴식 가구를 선택한다.
2. 펫이 해당 가구의 휴식 anchor로 이동한다.
3. 도착하면 `PetAction.RESTING`으로 변경한다.
4. `RESTING` 동안 `restRecoveryIntervalSeconds`마다 `vitality += restRecoveryAmount`를 적용한다.
5. `vitality = 100`이 되면 자동으로 `IDLE` 복귀하거나, 유저가 깨울 때까지 계속 쉰다.

MVP에서는 `vitality = 100` 도달 시 자동으로 `IDLE` 복귀를 추천한다.  
추후 연출이 중요해지면 유저가 깨울 때까지 쉬는 방식으로 바꿀 수 있다.

## 성격 반영

성격은 포만감/활력 수치 계산 자체보다는 행동 선택과 지속 시간에 반영한다.

| 성격 | 휴식 성향 | 자동 이동 | 권장 효과 |
|---|---|---|---|
| `ACTIVE` | 짧게 쉼 | 자주 움직임 | 활력이 낮아도 조금 더 버팀 |
| `CALM` | 보통 | 보통 | 기본 정책 |
| `LAZY` | 오래 쉼 | 덜 움직임 | 활력이 낮으면 더 자주 휴식 |

추후 자동 휴식이 들어가면 아래처럼 계산한다.

```text
자동 휴식 확률 = 활력 부족 보정 + 성격 보정
```

예시:

| 조건 | 자동 휴식 확률 |
|---|---:|
| `vitality >= 60` | 낮음 |
| `vitality 30~59` | 보통 |
| `vitality <= 29` | 높음 |
| `LAZY` | 확률 가산 |
| `ACTIVE` | 확률 감산 |

## 밤 수면 확장

`SLEEPING`은 지금 구현하지 않고, 전등/밤 시스템이 들어갈 때 추가한다.

예상 흐름:

1. 유저가 방 전등을 끈다.
2. 펫이 침대로 자동 이동한다.
3. 도착하면 `PetAction.SLEEPING`으로 변경한다.
4. 수면 중 포만감은 매우 천천히 감소한다.
5. 수면 중 활력은 빠르게 회복한다.
6. 전등을 켜거나, 아침 시간이 되거나, 활력이 충분하면 깨어난다.

`RESTING`과 `SLEEPING`을 분리하면 낮잠/휴식 가구와 밤 수면의 밸런스를 독립적으로 조정할 수 있다.

## API 계약 방향

추후 API도 아래 이름을 기준으로 사용한다.

```json
{
  "condition": {
    "satiety": 80,
    "vitality": 76,
    "isEgg": false,
    "updatedAt": "2026-05-09T18:30:00+09:00"
  }
}
```

먹이/휴식은 단순 상태 업데이트보다 행동 API로 분리하는 편이 좋다.

| 기능 | Method | URL | 설명 |
|---|---|---|---|
| 펫 조회 | GET | `/pet` | 현재 펫 상태 조회 |
| 먹이 주기 | POST | `/pet/feed` | 포만감 회복, 먹이 아이템/재화 차감 가능 |
| 휴식 시작 | POST | `/pet/rest/start` | 휴식 가구에서 `RESTING` 시작 |
| 휴식 종료 | POST | `/pet/rest/end` | 유저가 깨우거나 자동 종료 |
| 상태 동기화 | POST | `/pet/condition/sync` | 경과 시간 반영 후 현재 상태 반환 |

MVP에서는 기존 `updatePetStatus` 엔드포인트를 유지하되, 필드명은 `satiety/vitality` 기준으로 작성한다.

## 구현 순서 제안

1. `PetConditionPolicy`와 순수 계산 함수 추가
2. 데모 정책을 초 단위로 적용
3. 현재 `PetStatus`를 `satiety/vitality` 의미로 표시할 수 있게 UI 문구 정리
4. 먹이 행동에 포만감 회복 연결
5. 침대 휴식 행동에 `RESTING`과 활력 지속 회복 연결
6. 소파/빈백 등 휴식 가능 가구를 같은 `RESTING` 로직에 연결
7. 추후 전등/밤 시스템에서 `SLEEPING` 추가
