package com.example.lupapj.data.model

import kotlin.math.exp
import kotlin.random.Random

object BehaviorEvaluator {

    fun calculateDerivedTraits(traits: PetTraits): DerivedTraits {
        val vigor = (Math.pow(traits.activity.toDouble(), 0.8) * (0.5 + 0.5 * traits.curiosity)).toFloat().coerceIn(0f, 1f)
        val volatility = ((1f - traits.patience) * (0.5f + 0.5f * traits.curiosity)).coerceIn(0f, 1f)
        val restfulness = ((1f - traits.activity) * (0.5f + 0.5f * traits.patience)).coerceIn(0f, 1f)
        return DerivedTraits(vigor, volatility, restfulness)
    }

    fun calculateAffect(traits: PetTraits, derived: DerivedTraits, status: PetStatus): AffectState {
        val vHome = (0.3f + 0.2f * (derived.restfulness - 0.5f)).coerceIn(0f, 1f)
        val aHome = (0.2f + 0.7f * derived.vigor).coerceIn(0f, 1f)

        val valenceMod = ((status.satiety + status.vitality) / 200f - 0.5f) * 0.5f
        val arousalMod = ((100f - status.vitality) / 100f) * -0.2f

        return AffectState(
            valence = (vHome + valenceMod).coerceIn(0f, 1f),
            arousal = (aHome + arousalMod).coerceIn(0f, 1f)
        )
    }

    fun checkHazardExit(ticks: Float, patience: Float, random: Random = Random.Default): Boolean {
        val tau = 10f * (0.5f + patience) // base * (0.5 + patience)
        val dt = 1f // assuming 1 tick
        val probability = 1f - exp(-dt / tau).toFloat()
        return random.nextFloat() < probability
    }

    fun calculateUtilityScores(
        traits: PetTraits,
        derived: DerivedTraits,
        affect: AffectState,
        status: PetStatus,
        hasFood: Boolean,
        hasToy: Boolean,
        hasBed: Boolean,
        hasDroppedToy: Boolean = false,
        hasToyBox: Boolean = false
    ): Map<PetAction, Float> {
        val h = 100f - status.satiety
        val s = 100f - status.vitality
        val c = 100f - status.cleanliness

        val scores = mutableMapOf<PetAction, Float>()

        // [최종 교정] 기저(IDLE) 점수를 1.0으로 앵커링 (exp(2.5) ≈ 12.18의 강력한 중심축)
        // 중간값(0.5)일 때 대기 38%, 걷기 23%, 놀이 19%, 정리 8% 수준으로 자연 분배되는 최적의 계수 튜닝
        scores[PetAction.IDLE] = 1.0f
        
        if (hasFood) {
            val hungerMultiplier = if (status.satiety <= 30) 5.0f else 1.0f
            // 배고픔(h)이 20 이하일 경우 음수가 되어 수학적으로 확률이 소멸됨
            scores[PetAction.EATING] = ((h - 20f) / 80f) * (0.5f + traits.appetite) * 1.5f * hungerMultiplier
        }

        if (hasBed) {
            val laziness = 1f - traits.activity
            val exhaustionMultiplier = if (status.vitality <= 30) 4.0f else 1.0f
            
            // 바닥 쉬기: 피곤함(s)이 40 이하면 강한 음수, 즉 꽤 피곤하고 매우 게으를 때만 바닥에 눕게 됨
            scores[PetAction.RESTING] = ((s - 40f) / 60f) * (laziness * 4.0f) * exhaustionMultiplier
            
            // 침대 쉬기: 피곤함(s)이 10 이하면 음수. 조금만 피곤해도 침대를 선호함
            scores[PetAction.BED_RESTING] = ((s - 10f) / 90f) * 1.5f * exhaustionMultiplier
        } else {
            val exhaustionMultiplier = if (status.vitality <= 30) 4.0f else 1.0f
            // 침대가 없을 땐 s=20부터 바닥에 누울 수 있게 기준 완화
            scores[PetAction.RESTING] = ((s - 20f) / 80f) * (1.2f - traits.activity) * 1.5f * exhaustionMultiplier
        }

        if (hasToy) {
            scores[PetAction.PLAYING] = traits.curiosity * traits.activity * ((100f - s) / 100f) * 2.8f
        } else {
            // 장난감이 없어도 놀고 싶은 욕구 자체는 수식으로 잔존 (1~2% 수준)
            scores[PetAction.PLAYING] = traits.curiosity * traits.activity * ((100f - s) / 100f) * 0.2f
        }

        if (hasDroppedToy && hasToyBox) {
            scores[PetAction.CLEANING] = traits.attention * 0.7f
        }

        // 완전히 깨끗할 때(c < 20)는 음수가 되어 스스로 그루밍하지 않음
        val dirtyMultiplier = if (status.cleanliness <= 30) 4.0f else 1.0f
        scores[PetAction.GROOM] = ((c - 20f) / 80f) * traits.attention * 3.0f * dirtyMultiplier

        scores[PetAction.WALKING] = traits.activity * 1.6f

        return scores
    }

    fun calculateActionProbabilities(
        scores: Map<PetAction, Float>,
        patience: Float,
        volatility: Float
    ): Map<PetAction, Float> {
        val tBase = 0.4f
        val k = 0.2f
        val m = 0.2f
        val temperature = (tBase - k * patience + m * volatility).coerceIn(0.1f, 1.0f)

        // 아날로그 방식 복구: 어떠한 수동 필터링도 하지 않고 Softmax 고유의 수학적 연속성을 유지
        val expScores = scores.mapValues { kotlin.math.exp(it.value / temperature).toDouble() }
        val sumExp = expScores.values.sum()

        return expScores.mapValues { (it.value / sumExp).toFloat() }
    }

    fun sampleAction(
        probs: Map<PetAction, Float>,
        random: Random = Random.Default
    ): PetAction {
        var rand = random.nextFloat()
        for ((action, prob) in probs) {
            if (rand < prob) return action
            rand -= prob
        }

        return PetAction.IDLE
    }
}
