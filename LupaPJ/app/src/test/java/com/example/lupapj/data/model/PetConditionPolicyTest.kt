package com.example.lupapj.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetConditionPolicyTest {
    @Test
    fun advancePetCondition_decaysSatietyAndVitalityByDemoIntervals() {
        val result = advancePetCondition(
            status = PetStatus(satiety = 80, vitality = 80),
            action = PetAction.IDLE,
            elapsedSeconds = 10L,
            remainder = PetConditionTickRemainder()
        )

        assertEquals(74, result.status.satiety)
        assertEquals(76, result.status.vitality)
        assertEquals(96, result.status.cleanliness)
        assertEquals(0L, result.remainder.vitalityDecaySeconds)
    }

    @Test
    fun advancePetCondition_decaysCleanlinessByTwoEveryFiveSeconds() {
        val result = advancePetCondition(
            status = PetStatus(satiety = 80, vitality = 80, cleanliness = 80),
            action = PetAction.IDLE,
            elapsedSeconds = 5L,
            remainder = PetConditionTickRemainder()
        )

        assertEquals(78, result.status.cleanliness)
    }

    @Test
    fun advancePetCondition_carriesRemainderAcrossTicks() {
        val first = advancePetCondition(
            status = PetStatus(satiety = 80, vitality = 80),
            action = PetAction.IDLE,
            elapsedSeconds = 3L,
            remainder = PetConditionTickRemainder()
        )
        val second = advancePetCondition(
            status = first.status,
            action = PetAction.IDLE,
            elapsedSeconds = 7L,
            remainder = first.remainder
        )

        assertEquals(80, first.status.satiety)
        assertEquals(74, second.status.satiety)
    }

    @Test
    fun advancePetCondition_restingRecoversVitalityAndCanEndRest() {
        val result = advancePetCondition(
            status = PetStatus(satiety = 80, vitality = 97),
            action = PetAction.RESTING,
            elapsedSeconds = 20L,
            remainder = PetConditionTickRemainder()
        )

        assertEquals(100, result.status.vitality)
        assertTrue(result.shouldStopResting)
    }

    @Test
    fun advancePetCondition_idleDoesNotEndResting() {
        val result = advancePetCondition(
            status = PetStatus(satiety = 80, vitality = 80),
            action = PetAction.IDLE,
            elapsedSeconds = 4L,
            remainder = PetConditionTickRemainder()
        )

        assertFalse(result.shouldStopResting)
    }

    @Test
    fun applyFeedRecovery_restoresSatietyWithinBounds() {
        val result = applyFeedRecovery(
            status = PetStatus(satiety = 90, vitality = 80)
        )

        assertEquals(100, result.satiety)
        assertEquals(80, result.vitality)
    }
}
