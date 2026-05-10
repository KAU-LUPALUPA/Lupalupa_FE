package com.example.lupapj.data.model

import kotlin.math.exp
import kotlin.random.Random

/**
 * [추가됨(권)] 펫 행동 상태 기계 및 확률 전이 로직 유틸리티
 */
object BehaviorStateMachine {

    /**
     * 위기 상태(Crisis)에서의 전이 확률 변수 반환
     */
    fun getCrisisTransitionParams(personality: PetPersonality): Pair<Float, Float> {
        return when (personality) {
            PetPersonality.LAZY -> 0.80f to 0.10f
            PetPersonality.CALM -> 1.00f to 0.30f
            PetPersonality.ACTIVE -> 0.95f to 0.80f
        }
    }

    /**
     * [수정됨(권)] 아이템(사료/장난감) 발견 확률 변수 반환 (시간 경과에 따른 확률 상승 공식용)
     */
    fun getNoticeParams(personality: PetPersonality): Pair<Float, Float> {
        return when (personality) {
            PetPersonality.LAZY -> 0.60f to 0.08f   // 아주 천천히 발견 (최대 60%)
            PetPersonality.CALM -> 0.85f to 0.20f   // 보통 속도로 발견 (최대 85%)
            PetPersonality.ACTIVE -> 0.95f to 0.45f  // 매우 민감하게 발견 (최대 95%)
        }
    }

    /**
     * 안정 상태(Stable)에서의 전이(이탈) 확률 변수 반환
     */
    fun getStableTransitionParams(personality: PetPersonality, action: PetAction): Pair<Float, Float> {
        return when (personality) {
            PetPersonality.LAZY -> {
                when (action) {
                    PetAction.RESTING, PetAction.BED_RESTING -> 0.05f to 0.01f // [수정됨] 5% (표 기준)
                    PetAction.PLAYING -> 0.90f to 0.50f
                    else -> 0.80f to 0.10f
                }
            }
            PetPersonality.CALM -> 0.20f to 0.05f // [수정됨] 모든 행동 20%, 0.05
            PetPersonality.ACTIVE -> {
                when (action) {
                    PetAction.PLAYING -> 0.80f to 0.40f // [수정됨] 80%, 0.4
                    else -> 0.95f to 0.80f // 기본값
                }
            }
        }
    }

    /**
     * 점근적 확률 모델 P(n) = M * (1 - e^(-k * n))
     * @param n 연속 조건 충족 횟수 (틱 수)
     * @param m 최대 수렴 확률 (0.0 ~ 1.0)
     * @param k 반응 민감도 상수
     * @return 전이 성공 여부
     */
    fun checkTransitionProbability(n: Int, m: Float, k: Float, random: Random = Random.Default): Boolean {
        val probability = m * (1f - exp(-k * n)).toFloat()
        return random.nextFloat() < probability
    }

    /**
     * 위기 상태 여부 반환
     */
    fun isCrisis(satiety: Int, vitality: Int): Boolean {
        return satiety < 30 || vitality < 30
    }

    /**
     * 안정 상태 행동 가중치 룰렛 실행
     * @param hasFood 바닥에 사료가 있는지 여부
     * @param hasToy 바닥에 장난감이 있는지 여부
     * @param satiety 현재 포만감 수치 (사료 섭취 의지 계산용)
     */
    fun rollStableBehavior(
        personality: PetPersonality,
        satiety: Int,
        hasFood: Boolean,
        hasToy: Boolean,
        random: Random = Random.Default
    ): PetAction {
        val weights = when (personality) {
            PetPersonality.LAZY -> mutableListOf(
                PetAction.RESTING to 90,
                PetAction.PLAYING to 3,
                PetAction.WALKING to 7
            )
            PetPersonality.CALM -> mutableListOf(
                PetAction.RESTING to 20,
                PetAction.PLAYING to 50,
                PetAction.WALKING to 30
            )
            PetPersonality.ACTIVE -> mutableListOf(
                PetAction.RESTING to 0,
                PetAction.PLAYING to 20,
                PetAction.WALKING to 80
            )
        }

        // [추가됨] 사료가 바닥에 있을 경우, 배고픈 정도에 비례하여 식사 가중치 부여
        if (hasFood) {
            val hungerWeight = (100 - satiety).coerceIn(0, 100)
            // 성격별 식탐 가중치 보정 (LAZY는 식탐이 더 많음)
            val appetiteMultiplier = when (personality) {
                PetPersonality.LAZY -> 1.5f
                PetPersonality.CALM -> 1.0f
                PetPersonality.ACTIVE -> 0.8f
            }
            weights.add(PetAction.EATING to (hungerWeight * appetiteMultiplier).toInt())
        }

        // [수정됨(권)] 장난감이 없는 경우 놀이 가중치 감소 (장난감 박스에서 꺼내 노는 것은 낮은 확률로 유지)
        if (!hasToy) {
            val playingIndex = weights.indexOfFirst { it.first == PetAction.PLAYING }
            if (playingIndex != -1) {
                val (action, weight) = weights[playingIndex]
                weights[playingIndex] = action to (weight * 0.3f).toInt() 
            }
        }

        // TODO: PetAction을 단순 Enum이 아닌 Sealed Class화 하여 파라미터를 포함하도록 개선 고려

        val totalWeight = weights.sumOf { it.second }
        if (totalWeight <= 0) return PetAction.IDLE

        var randomVal = random.nextInt(totalWeight)
        for ((action, weight) in weights) {
            if (randomVal < weight) return action
            randomVal -= weight
        }
        return PetAction.IDLE
    }
}
