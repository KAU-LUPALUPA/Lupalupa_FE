package com.example.lupapj.data.model.scene

import com.example.lupapj.data.mock.DemoScenes
import com.example.lupapj.data.model.PetPersonality
import kotlin.math.hypot
import kotlin.random.Random
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetAutonomousMovementTest {
    @Test
    fun chooseAutonomousPetTarget_staysInsidePersonalityRadiusAndWalkableArea() {
        val currentAnchor = FloorAnchor(u = 0.44f, v = 0.64f)
        val profile = autonomousMovementProfileFor(PetPersonality.ACTIVE)
        val random = Random(7)

        repeat(30) {
            val target = chooseAutonomousPetTarget(
                currentAnchor = currentAnchor,
                sceneDefinition = DemoScenes.mainRoom,
                profile = profile,
                random = random
            )

            assertNotNull(target)
            requireNotNull(target)
            assertTrue(target.isWalkableIn(DemoScenes.mainRoom))
            assertTrue(target.distanceTo(currentAnchor) <= profile.radius + 0.0001f)
        }
    }

    @Test
    fun autonomousMovementProfileFor_usesWiderRadiusForMoreActivePersonalities() {
        val active = autonomousMovementProfileFor(PetPersonality.ACTIVE)
        val calm = autonomousMovementProfileFor(PetPersonality.CALM)
        val lazy = autonomousMovementProfileFor(PetPersonality.LAZY)

        assertTrue(active.radius > calm.radius)
        assertTrue(calm.radius > lazy.radius)
        assertTrue(active.maxIdleDelayMillis < lazy.maxIdleDelayMillis)
    }
}

private fun FloorAnchor.distanceTo(other: FloorAnchor): Float {
    return hypot(u - other.u, v - other.v)
}
