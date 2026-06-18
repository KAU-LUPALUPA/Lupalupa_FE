package com.example.lupapj.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BehaviorEvaluatorTest {
    @Test
    fun calculateDerivedTraits_mapsRestfulnessToRestfulnessField() {
        val derived = BehaviorEvaluator.calculateDerivedTraits(
            PetTraits(activity = 0f, curiosity = 0.5f, patience = 1f)
        )

        assertEquals(1f, derived.restfulness, 0.0001f)
        assertEquals(0.5f, derived.tidiness, 0.0001f)
    }

    @Test
    fun calculateUtilityScores_raisesGroomingScoreAsCleanlinessDrops() {
        val cleanScore = groomingScore(cleanliness = 90)
        val slightlyDirtyScore = groomingScore(cleanliness = 70)
        val dirtyScore = groomingScore(cleanliness = 50)
        val veryDirtyScore = groomingScore(cleanliness = 35)

        assertTrue(slightlyDirtyScore > cleanScore)
        assertTrue(dirtyScore > slightlyDirtyScore)
        assertTrue(veryDirtyScore > dirtyScore)
    }

    @Test
    fun calculateActionProbabilities_makesGroomingDominantWhenVeryDirty() {
        val traits = PetTraits()
        val derived = BehaviorEvaluator.calculateDerivedTraits(traits)
        val scores = BehaviorEvaluator.calculateUtilityScores(
            traits = traits,
            derived = derived,
            affect = AffectState(valence = 0.5f, arousal = 0.5f),
            status = PetStatus(satiety = 80, vitality = 80, cleanliness = 35),
            hasFood = true,
            hasToy = true,
            hasBed = true,
            hasDroppedToy = false,
            hasToyBox = true
        )
        val probabilities = BehaviorEvaluator.calculateActionProbabilities(
            scores = scores,
            patience = traits.patience,
            volatility = derived.volatility
        )

        assertEquals(PetAction.GROOM, probabilities.maxByOrNull { it.value }?.key)
    }

    private fun groomingScore(cleanliness: Int): Float {
        val traits = PetTraits()
        val derived = BehaviorEvaluator.calculateDerivedTraits(traits)
        return BehaviorEvaluator.calculateUtilityScores(
            traits = traits,
            derived = derived,
            affect = AffectState(valence = 0.5f, arousal = 0.5f),
            status = PetStatus(satiety = 80, vitality = 80, cleanliness = cleanliness),
            hasFood = true,
            hasToy = true,
            hasBed = true
        ).getValue(PetAction.GROOM)
    }
}
