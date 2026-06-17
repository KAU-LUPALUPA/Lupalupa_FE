package com.example.lupapj.data.model.scene

import com.example.lupapj.data.model.PetTraits
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

const val PET_AUTONOMOUS_MOVE_DURATION_MS = 900L

private const val TARGET_ATTEMPT_COUNT = 24
private const val MIN_TARGET_DISTANCE = 0.045f

data class PetAutonomousMovementProfile(
    val radius: Float,
    val minIdleDelayMillis: Long,
    val maxIdleDelayMillis: Long,
    val bouncePx: Float = 0f, // [추가됨(권)] 바운스 연출
    val speedMultiplier: Float = 1.0f, // [추가됨(권)] 이동 속도 배율
    val style: PetMovementStyle = PetMovementStyle.BOUNCY
) {
    fun nextIdleDelayMillis(random: Random): Long {
        if (minIdleDelayMillis >= maxIdleDelayMillis) return minIdleDelayMillis
        return random.nextLong(minIdleDelayMillis, maxIdleDelayMillis + 1L)
    }
}

fun autonomousMovementProfileFor(
    traits: PetTraits
): PetAutonomousMovementProfile {
    return when {
        traits.activity > 0.6f -> PetAutonomousMovementProfile(
            radius = 0.55f,
            minIdleDelayMillis = 1_500L,
            maxIdleDelayMillis = 3_200L,
            bouncePx = 6f,
            speedMultiplier = 1.2f
        )
        traits.activity < 0.3f -> PetAutonomousMovementProfile(
            radius = 0.15f,
            minIdleDelayMillis = 5_500L,
            maxIdleDelayMillis = 8_500L,
            bouncePx = 0f,
            speedMultiplier = 1.0f
        )
        else -> PetAutonomousMovementProfile(
            radius = 0.35f,
            minIdleDelayMillis = 3_000L,
            maxIdleDelayMillis = 5_500L,
            bouncePx = 4f,
            speedMultiplier = 1.0f
        )
    }
}

fun chooseAutonomousPetTarget(
    currentAnchor: FloorAnchor,
    sceneDefinition: RoomSceneDefinition,
    profile: PetAutonomousMovementProfile,
    random: Random = Random.Default
): FloorAnchor? {
    val tileCandidates = sceneDefinition.tileCenterCandidates()
        .filter { candidate ->
            candidate.distanceTo(currentAnchor) in MIN_TARGET_DISTANCE..profile.radius &&
                candidate.isWalkableIn(sceneDefinition)
        }

    if (tileCandidates.isNotEmpty()) {
        return tileCandidates[random.nextInt(tileCandidates.size)]
    }

    repeat(TARGET_ATTEMPT_COUNT) {
        val candidate = randomAnchorInsideRadius(
            center = currentAnchor,
            radius = profile.radius,
            random = random
        )
        if (
            candidate.distanceTo(currentAnchor) >= MIN_TARGET_DISTANCE &&
            candidate.isWalkableIn(sceneDefinition)
        ) {
            return candidate
        }
    }

    return null
}

fun FloorAnchor.isWalkableIn(sceneDefinition: RoomSceneDefinition): Boolean {
    if (u !in 0f..1f || v !in 0f..1f) return false

    val navigationSpec = sceneDefinition.navigationSpec ?: return true
    val insideWalkableArea = navigationSpec.walkableAreas.isEmpty() ||
        navigationSpec.walkableAreas.any { it.contains(this) }
    val insideBlockedZone = navigationSpec.blockedZones.any { it.contains(this) }

    return insideWalkableArea && !insideBlockedZone
}

private fun RoomSceneDefinition.tileCenterCandidates(): List<FloorAnchor> {
    val columns = projectionSpec.roomWidthTiles.roundToInt().coerceAtLeast(1)
    val rows = projectionSpec.roomDepthTiles.roundToInt().coerceAtLeast(1)

    return buildList {
        for (column in 0 until columns) {
            for (row in 0 until rows) {
                add(
                    FloorAnchor(
                        u = (column + 0.5f) / columns,
                        v = (row + 0.5f) / rows
                    )
                )
            }
        }
    }
}

private fun randomAnchorInsideRadius(
    center: FloorAnchor,
    radius: Float,
    random: Random
): FloorAnchor {
    val angle = random.nextFloat() * (PI.toFloat() * 2f)
    val distance = sqrt(random.nextFloat()) * radius

    return FloorAnchor(
        u = center.u + (cos(angle) * distance),
        v = center.v + (sin(angle) * distance)
    )
}

private fun FloorAnchor.distanceTo(other: FloorAnchor): Float {
    return hypot(u - other.u, v - other.v)
}

private fun SceneCollisionRect.contains(anchor: FloorAnchor): Boolean {
    return anchor.u in minU..maxU && anchor.v in minV..maxV
}
