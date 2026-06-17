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
        hasBed: Boolean
    ): Map<PetAction, Float> {
        val h = 100f - status.satiety
        val s = 100f - status.vitality

        val scores = mutableMapOf<PetAction, Float>()

        scores[PetAction.IDLE] = 0.1f

        if (hasFood) {
            scores[PetAction.EATING] = (h / 100f) * (0.5f + traits.appetite)
        }

        if (hasBed) {
            scores[PetAction.RESTING] = (s / 100f) * (1.2f - traits.activity) * 1.5f
            scores[PetAction.BED_RESTING] = (s / 100f) * (1.2f - traits.activity) * 2.0f
        } else {
            scores[PetAction.RESTING] = (s / 100f) * (1.2f - traits.activity)
        }

        if (hasToy) {
            scores[PetAction.PLAYING] = traits.curiosity * traits.activity * ((100f - s) / 100f) * 1.5f
        } else {
            scores[PetAction.PLAYING] = traits.curiosity * traits.activity * ((100f - s) / 100f) * 0.3f
        }

        scores[PetAction.WALKING] = traits.activity * 0.5f

        return scores
    }

    fun sampleAction(
        scores: Map<PetAction, Float>,
        patience: Float,
        volatility: Float,
        random: Random = Random.Default
    ): PetAction {
        val tBase = 1.0f
        val k = 0.5f
        val m = 0.5f
        val temperature = (tBase - k * patience + m * volatility).coerceIn(0.1f, 2.0f)

        val expScores = scores.mapValues { exp(it.value / temperature).toDouble() }
        val sumExp = expScores.values.sum()

        var rand = random.nextDouble() * sumExp
        for ((action, weight) in expScores) {
            if (rand < weight) return action
            rand -= weight
        }

        return PetAction.IDLE
    }
}
