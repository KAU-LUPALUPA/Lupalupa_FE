package com.example.lupapj.data.model

import kotlin.math.roundToInt

data class PetConditionPolicy(
    val satietyDecayIntervalSeconds: Long,
    val satietyDecayAmount: Int,
    val vitalityDecayIntervalSeconds: Long,
    val vitalityDecayAmount: Int,
    val feedRecoveryAmount: Int,
    val restRecoveryIntervalSeconds: Long,
    val floorRestRecoveryAmount: Int,
    val bedRestRecoveryAmount: Int,
    val restSatietyDecayMultiplier: Float
)

data class PetConditionTickRemainder(
    val satietyDecaySeconds: Long = 0L,
    val vitalityDecaySeconds: Long = 0L,
    val restRecoverySeconds: Long = 0L
)

data class PetConditionTickResult(
    val status: PetStatus,
    val remainder: PetConditionTickRemainder,
    val shouldStopResting: Boolean = false
)

val DemoPetConditionPolicy = PetConditionPolicy(
    satietyDecayIntervalSeconds = 10L,
    satietyDecayAmount = 2,
    vitalityDecayIntervalSeconds = 10L,
    vitalityDecayAmount = 1,
    feedRecoveryAmount = 30,
    restRecoveryIntervalSeconds = 10L,
    floorRestRecoveryAmount = 2,
    bedRestRecoveryAmount = 6,
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

    val satietyTick = consumeInterval(
        currentSeconds = remainder.satietyDecaySeconds,
        addedSeconds = elapsedSeconds,
        intervalSeconds = policy.satietyDecayIntervalSeconds
    )
    val satietyDecayAmount = satietyDecayAmountFor(
        action = action,
        steps = satietyTick.steps,
        policy = policy
    )
    val nextSatiety = status.satiety - satietyDecayAmount

    val vitalityResult = if (action == PetAction.RESTING || action == PetAction.BED_RESTING) {
        val restTick = consumeInterval(
            currentSeconds = remainder.restRecoverySeconds,
            addedSeconds = elapsedSeconds,
            intervalSeconds = policy.restRecoveryIntervalSeconds
        )
        val recoveryAmount = if (action == PetAction.BED_RESTING) policy.bedRestRecoveryAmount else policy.floorRestRecoveryAmount
        val nextVitality = status.vitality + (restTick.steps * recoveryAmount)
        VitalityAdvanceResult(
            vitality = nextVitality,
            // [수정됨(권)] 상태 전환 시에도 기존 타이머 잔여 시간을 보존하여 포만감/활력 박자가 어긋나는 현상 방지
            vitalityDecaySeconds = remainder.vitalityDecaySeconds,
            restRecoverySeconds = restTick.remainingSeconds
        )
    } else {
        val vitalityTick = consumeInterval(
            currentSeconds = remainder.vitalityDecaySeconds,
            addedSeconds = elapsedSeconds,
            intervalSeconds = policy.vitalityDecayIntervalSeconds
        )
        val nextVitality = status.vitality - (vitalityTick.steps * policy.vitalityDecayAmount)
        VitalityAdvanceResult(
            vitality = nextVitality,
            vitalityDecaySeconds = vitalityTick.remainingSeconds,
            // [수정됨(권)] 일반 상태에서도 기존 휴식 회복 잔여 타이머를 보존함
            restRecoverySeconds = remainder.restRecoverySeconds
        )
    }

    val nextStatus = status.copy(
        satiety = nextSatiety,
        vitality = vitalityResult.vitality
    ).coerced()

    return PetConditionTickResult(
        status = nextStatus,
        remainder = PetConditionTickRemainder(
            satietyDecaySeconds = satietyTick.remainingSeconds,
            vitalityDecaySeconds = vitalityResult.vitalityDecaySeconds,
            restRecoverySeconds = vitalityResult.restRecoverySeconds
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

private fun satietyDecayAmountFor(
    action: PetAction,
    steps: Int,
    policy: PetConditionPolicy
): Int {
    if (steps <= 0) return 0

    val baseAmount = steps * policy.satietyDecayAmount
    if (action != PetAction.RESTING && action != PetAction.BED_RESTING) return baseAmount

    return (baseAmount * policy.restSatietyDecayMultiplier)
        .roundToInt()
        .coerceAtLeast(1)
}

private fun PetStatus.coerced(): PetStatus {
    return copy(
        satiety = satiety.coerceIn(0, 100),
        vitality = vitality.coerceIn(0, 100)
    )
}
