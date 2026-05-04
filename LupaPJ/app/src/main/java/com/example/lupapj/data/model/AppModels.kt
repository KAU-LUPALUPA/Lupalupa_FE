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
    ROOM,
    GALLERY, // [추가됨] 갤러리 화면 상태 추가
    FRIENDS,
    FRIEND_ROOM,
    SHOP,        // [추가됨(권)] 상점 메인 화면 페이즈. 상점 하단 탭 클릭 시 진입.
    SHOP_DETAIL, // [추가됨(권)] 상점 아이템 상세 및 치장 미리보기 화면 페이즈. 아이템 클릭 시 진입.
    MINIGAME     // [추가됨(권)] 미니게임 플레이 화면 페이즈.
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
    val inventoryVisible: Boolean = false,
    val isCameraMode: Boolean = false, // [추가됨] 스크린샷 카메라 모드 활성화 여부
    val cameraZoom: Float = 1f // [추가됨] 카메라 줌 배율 (기본 1x)
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
        BottomNavItem.CONTACTS -> "친구"
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

// [추가됨] 갤러리 이미지 데이터 모델. 서버 연동 확장을 위해 id 필드 포함
data class GalleryImage(
    val id: String,
    val filePath: String,
    val isFavorite: Boolean = false,
    val timestamp: Long
)

// [추가됨(권)] 재화 상태 모델. 현재는 단일 재화(amount)만 관리함.
data class CurrencyState(
    val amount: Int = 0
)

// [추가됨(권)] 상점 아이템의 카테고리 (치장 부위 구분을 위해 사용)
enum class ShopCategory {
    HAT,
    GLASSES,
    CLOTHING,
    ACCESSORY
}

// [추가됨(권)] 상점 카테고리를 UI에 표시하기 위한 한국어 라벨 확장 프로퍼티
val ShopCategory.label: String
    get() = when (this) {
        ShopCategory.HAT -> "모자"
        ShopCategory.GLASSES -> "안경"
        ShopCategory.CLOTHING -> "옷"
        ShopCategory.ACCESSORY -> "액세서리"
    }

// [추가됨(권)] 상점 아이템 모델. 구매 시 필요한 재화(price) 및 미리보기 오버레이 리소스(previewOverlayResId) 포함.
data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val category: ShopCategory,
    val thumbnailResId: Int? = null,
    val previewOverlayResId: Int? = null
)
