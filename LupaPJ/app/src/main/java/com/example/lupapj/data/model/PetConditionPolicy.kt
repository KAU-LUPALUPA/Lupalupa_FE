package com.example.lupapj.data.model

import kotlin.math.roundToInt

data class PetConditionPolicy(
    val satietyDecayIntervalSeconds: Long,
    val satietyDecayAmount: Int,
    val vitalityDecayIntervalSeconds: Long,
    val vitalityDecayAmount: Int,
    val cleanlinessDecayIntervalSeconds: Long, // [추가됨]
    val cleanlinessDecayAmount: Int, // [추가됨]
    val feedRecoveryAmount: Int,
    val restRecoveryIntervalSeconds: Long,
    val floorRestRecoveryAmount: Int,
    val bedRestRecoveryAmount: Int,
    val restSatietyDecayMultiplier: Float
)

data class PetConditionTickRemainder(
    val satietyDecaySeconds: Long = 0L,
    val vitalityDecaySeconds: Long = 0L,
    val cleanlinessDecaySeconds: Long = 0L, // [추가됨]
    val restRecoverySeconds: Long = 0L
)

data class PetConditionTickResult(
    val status: PetStatus,
    val remainder: PetConditionTickRemainder,
    val shouldStopResting: Boolean = false
)

// 테스트 및 데모용으로 차감/회복 속도를 대폭 상향
val DemoPetConditionPolicy = PetConditionPolicy(
    satietyDecayIntervalSeconds = 5L, // 10 -> 5초
    satietyDecayAmount = 3,           // 2 -> 3
    vitalityDecayIntervalSeconds = 5L, // 10 -> 5초
    vitalityDecayAmount = 2,           // 1 -> 2
    cleanlinessDecayIntervalSeconds = 5L, // [추가됨]
    cleanlinessDecayAmount = 1,           // [추가됨]
    feedRecoveryAmount = 30,
    restRecoveryIntervalSeconds = 5L, // 10 -> 5초
    floorRestRecoveryAmount = 3,      // 2 -> 3
    bedRestRecoveryAmount = 8,        // 6 -> 8
    restSatietyDecayMultiplier = 1.0f
)

fun advancePetCondition(
    status: PetStatus,
    action: PetAction,
    elapsedSeconds: Long,
    remainder: PetConditionTickRemainder,
    policy: PetConditionPolicy = DemoPetConditionPolicy
): PetConditionTickResult {
    if (elapsedSeconds <= 0L || status.isEgg) {
        return PetConditionTickResult(
            status = status.coerced(),
            remainder = remainder
        )
    }

    // 1. 기본 자연 차감 (숨만 쉬어도 무조건 깎임)
    val satietyTick = consumeInterval(
        currentSeconds = remainder.satietyDecaySeconds,
        addedSeconds = elapsedSeconds,
        intervalSeconds = policy.satietyDecayIntervalSeconds
    )
    val baseSatietyDecay = satietyTick.steps * policy.satietyDecayAmount

    val vitalityTick = consumeInterval(
        currentSeconds = remainder.vitalityDecaySeconds,
        addedSeconds = elapsedSeconds,
        intervalSeconds = policy.vitalityDecayIntervalSeconds
    )
    val baseVitalityDecay = vitalityTick.steps * policy.vitalityDecayAmount

    val cleanlinessTick = consumeInterval(
        currentSeconds = remainder.cleanlinessDecaySeconds,
        addedSeconds = elapsedSeconds,
        intervalSeconds = policy.cleanlinessDecayIntervalSeconds
    )
    val baseCleanlinessDecay = cleanlinessTick.steps * policy.cleanlinessDecayAmount

    // 2. 행동별 추가 수치 (표 기준 Extra)
    val extraSatietyDecay = extraSatietyDecayAmountFor(action, satietyTick.steps, policy)
    val extraVitalityDecay = extraVitalityDecayAmountFor(action, vitalityTick.steps, policy)
    val extraCleanlinessDecay = extraCleanlinessDecayAmountFor(action, cleanlinessTick.steps, policy)

    // 3. 휴식 회복 계산 (침대/바닥)
    val restTick = consumeInterval(
        currentSeconds = remainder.restRecoverySeconds,
        addedSeconds = elapsedSeconds,
        intervalSeconds = policy.restRecoveryIntervalSeconds
    )
    val vitalityRecovery = if (action == PetAction.BED_RESTING) {
        restTick.steps * policy.bedRestRecoveryAmount
    } else if (action == PetAction.RESTING) {
        restTick.steps * policy.floorRestRecoveryAmount
    } else {
        0
    }

    val nextSatiety = status.satiety - baseSatietyDecay - extraSatietyDecay
    val nextVitality = status.vitality - baseVitalityDecay - extraVitalityDecay + vitalityRecovery
    val nextCleanliness = status.cleanliness - baseCleanlinessDecay - extraCleanlinessDecay

    val nextStatus = status.copy(
        satiety = nextSatiety,
        vitality = nextVitality,
        cleanliness = nextCleanliness
    ).coerced()

    return PetConditionTickResult(
        status = nextStatus,
        remainder = PetConditionTickRemainder(
            satietyDecaySeconds = satietyTick.remainingSeconds,
            vitalityDecaySeconds = vitalityTick.remainingSeconds,
            cleanlinessDecaySeconds = cleanlinessTick.remainingSeconds,
            restRecoverySeconds = restTick.remainingSeconds
        ),
        shouldStopResting = (action == PetAction.RESTING || action == PetAction.BED_RESTING) && nextStatus.vitality == 100
    )
}

fun applyFeedRecovery(
    status: PetStatus,
    policy: PetConditionPolicy = DemoPetConditionPolicy
): PetStatus {
    return status.copy(
        satiety = status.satiety + policy.feedRecoveryAmount
    ).coerced()
}

private data class IntervalConsumption(
    val steps: Int,
    val remainingSeconds: Long
)

private data class VitalityAdvanceResult(
    val vitality: Int,
    val vitalityDecaySeconds: Long,
    val restRecoverySeconds: Long
)

private fun consumeInterval(
    currentSeconds: Long,
    addedSeconds: Long,
    intervalSeconds: Long
): IntervalConsumption {
    val safeInterval = intervalSeconds.coerceAtLeast(1L)
    val totalSeconds = (currentSeconds + addedSeconds).coerceAtLeast(0L)

    return IntervalConsumption(
        steps = (totalSeconds / safeInterval).toInt(),
        remainingSeconds = totalSeconds % safeInterval
    )
}

private fun extraSatietyDecayAmountFor(
    action: PetAction,
    steps: Int,
    policy: PetConditionPolicy
): Int {
    if (steps <= 0) return 0
    return when (action) {
        // 표 기준: 돌아다니기, 장난감 놀기, 장난감 정리 시 추가 감소
        PetAction.WALKING, PetAction.PLAYING, PetAction.CLEANING -> steps * policy.satietyDecayAmount
        // 그 외에는 기본 감소만 적용됨
        else -> 0
    }
}

private fun extraVitalityDecayAmountFor(
    action: PetAction,
    steps: Int,
    policy: PetConditionPolicy
): Int {
    if (steps <= 0) return 0
    return when (action) {
        // 표 기준: 돌아다니기, 밥먹기, 장난감 놀기, 그루밍, 장난감 정리 시 추가 감소
        PetAction.WALKING, PetAction.EATING, PetAction.PLAYING, PetAction.GROOM, PetAction.CLEANING -> steps * policy.vitalityDecayAmount
        else -> 0
    }
}

private fun extraCleanlinessDecayAmountFor(
    action: PetAction,
    steps: Int,
    policy: PetConditionPolicy
): Int {
    if (steps <= 0) return 0
    return when (action) {
        // 표 기준: 밥먹기, 바닥에서 자기, 장난감 놀기 시 추가 감소
        PetAction.EATING, PetAction.RESTING, PetAction.PLAYING -> steps * policy.cleanlinessDecayAmount
        else -> 0
    }
}

private fun PetStatus.coerced(): PetStatus {
    return copy(
        satiety = satiety.coerceIn(0, 100),
        vitality = vitality.coerceIn(0, 100),
        cleanliness = cleanliness.coerceIn(0, 100) // [추가됨]
    )
}
