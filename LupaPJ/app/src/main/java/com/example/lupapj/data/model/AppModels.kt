package com.example.lupapj.data.model

import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.FloorPlaneSpec
import com.example.lupapj.data.model.scene.HouseSceneState
import com.example.lupapj.data.model.scene.RoomSceneDefinition
import com.example.lupapj.data.model.scene.SceneAnchor
import com.example.lupapj.data.model.scene.SceneFractionPoint
import com.example.lupapj.data.model.scene.SceneObjectDefinition
import com.example.lupapj.data.model.scene.WallAnchor
import com.example.lupapj.data.model.scene.WallFace
import com.example.lupapj.data.model.scene.WallPlaneSpec
import com.example.lupapj.data.model.scene.initialHouseSceneState

enum class AppPhase {
    MAIN_LOADING,
    ROOM
}

/**
 * Legacy viewport coordinate retained during migration away from direct screen fractions.
 * New room rendering should prefer FloorAnchor / WallAnchor instead.
 */
data class RoomPoint(
    val xFraction: Float,
    val yFraction: Float
)

data class AuthSession(
    val userId: String
)

enum class RoomObjectType {
    BED,
    TOY_BOX,
    FOOD_BAG,
    WINDOW
}

enum class PetAction {
    IDLE,
    RESTING,
    PLAYING,
    EATING
}

data class PetUiState(
    val currentAction: PetAction = PetAction.IDLE,
    val position: RoomPoint = RoomPoint(
        xFraction = 0.44f,
        yFraction = 0.68f
    )
)

data class RoomObjectUi(
    val id: String,
    val type: RoomObjectType,
    val label: String,
    val position: RoomPoint
)

data class RoomUiState(
    val sceneDefinition: RoomSceneDefinition,
    val houseSceneState: HouseSceneState,
    val feedMode: Boolean = false,
    val toyMode: Boolean = false,
    val navBarVisible: Boolean = false,
    val inventoryVisible: Boolean = false
) {
    val statusText: String
        get() = when {
            feedMode -> "먹이 줄 위치를 바닥에서 선택하세요."
            toyMode -> "장난감을 놓을 위치를 바닥에서 선택하세요."
            else -> houseSceneState.pet.action.label
        }

    val droppedFoodAnchor: FloorAnchor?
        get() = houseSceneState.currentSceneRuntime.droppedFoodAnchor

    val droppedToyAnchor: FloorAnchor?
        get() = houseSceneState.currentSceneRuntime.droppedToyAnchor

    // Legacy adapters kept while older preview/debug helpers still depend on RoomPoint-based types.
    val foodPosition: RoomPoint?
        get() = droppedFoodAnchor?.toLegacyRoomPoint(sceneDefinition)

    val pet: PetUiState
        get() = PetUiState(
            currentAction = houseSceneState.pet.action,
            position = houseSceneState.pet.anchor.toLegacyRoomPoint(sceneDefinition)
        )

    val roomObjects: List<RoomObjectUi>
        get() = (sceneDefinition.fixedDecor + sceneDefinition.objects).map {
            it.toLegacyRoomObjectUi(sceneDefinition)
        }
}

enum class BottomNavItem {
    SHOP,
    SCREENSHOT,
    CONTACTS,
    GALLERY
}

val PetAction.label: String
    get() = when (this) {
        PetAction.IDLE -> "대기 중"
        PetAction.RESTING -> "휴식 중"
        PetAction.PLAYING -> "노는 중"
        PetAction.EATING -> "먹는 중"
    }

val BottomNavItem.label: String
    get() = when (this) {
        BottomNavItem.SHOP -> "상점"
        BottomNavItem.SCREENSHOT -> "스크린샷"
        BottomNavItem.CONTACTS -> "연락처"
        BottomNavItem.GALLERY -> "갤러리"
    }

val RoomObjectType.label: String
    get() = when (this) {
        RoomObjectType.BED -> "침대"
        RoomObjectType.TOY_BOX -> "장난감박스"
        RoomObjectType.FOOD_BAG -> "사료 봉투"
        RoomObjectType.WINDOW -> "창문"
    }

fun initialRoomUiState(
    sceneDefinition: RoomSceneDefinition,
    houseSceneState: HouseSceneState = initialHouseSceneState(sceneDefinition.id)
): RoomUiState {
    return RoomUiState(
        sceneDefinition = sceneDefinition,
        houseSceneState = houseSceneState
    )
}

private fun SceneObjectDefinition.toLegacyRoomObjectUi(
    sceneDefinition: RoomSceneDefinition
): RoomObjectUi {
    return RoomObjectUi(
        id = id,
        type = type,
        label = sprite.fallbackLabel,
        position = anchor.toLegacyRoomPoint(sceneDefinition)
    )
}

fun SceneAnchor.toLegacyRoomPoint(sceneDefinition: RoomSceneDefinition): RoomPoint {
    return when (this) {
        is FloorAnchor -> toLegacyFloorPoint(sceneDefinition.floorPlane)
        is WallAnchor -> toLegacyWallPoint(sceneDefinition.wallPlaneFor(face))
    }
}

private fun FloorAnchor.toLegacyFloorPoint(
    plane: FloorPlaneSpec
): RoomPoint {
    val leftEdge = lerpPoint(plane.backLeftCorner, plane.frontLeftCorner, v)
    val rightEdge = lerpPoint(plane.backRightCorner, plane.frontRightCorner, v)
    val point = lerpPoint(leftEdge, rightEdge, u)
    return RoomPoint(
        xFraction = point.xFraction,
        yFraction = point.yFraction
    )
}

private fun WallAnchor.toLegacyWallPoint(
    plane: WallPlaneSpec
): RoomPoint {
    val leftEdge = lerpPoint(plane.topLeftCorner, plane.bottomLeftCorner, v)
    val rightEdge = lerpPoint(plane.topRightCorner, plane.bottomRightCorner, v)
    val point = lerpPoint(leftEdge, rightEdge, u)
    return RoomPoint(
        xFraction = point.xFraction,
        yFraction = point.yFraction
    )
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

private fun lerpPoint(
    start: SceneFractionPoint,
    end: SceneFractionPoint,
    fraction: Float
): SceneFractionPoint {
    return SceneFractionPoint(
        xFraction = lerp(start.xFraction, end.xFraction, fraction),
        yFraction = lerp(start.yFraction, end.yFraction, fraction)
    )
}

private fun RoomSceneDefinition.wallPlaneFor(face: WallFace): WallPlaneSpec {
    return when (face) {
        WallFace.BACK -> wallPlane
        WallFace.LEFT,
        WallFace.RIGHT -> sideWallPlane ?: wallPlane
    }
}
