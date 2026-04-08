package com.example.lupapj.data.model

enum class AppPhase {
    MAIN_LOADING,
    ROOM
}

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
    val feedMode: Boolean = false,
    val navBarVisible: Boolean = false,
    val inventoryVisible: Boolean = false,
    val foodPosition: RoomPoint? = null,
    val roomObjects: List<RoomObjectUi> = defaultRoomObjects(),
    val pet: PetUiState = defaultPetUiState(),
    val statusText: String = PetAction.IDLE.label
)

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

fun defaultPetUiState(): PetUiState {
    return PetUiState(
        currentAction = PetAction.IDLE,
        position = RoomPoint(
            xFraction = 0.44f,
            yFraction = 0.68f
        )
    )
}

fun defaultRoomObjects(): List<RoomObjectUi> {
    return listOf(
        RoomObjectUi(
            id = "bed",
            type = RoomObjectType.BED,
            label = RoomObjectType.BED.label,
            position = RoomPoint(0.18f, 0.58f)
        ),
        RoomObjectUi(
            id = "toy_box",
            type = RoomObjectType.TOY_BOX,
            label = RoomObjectType.TOY_BOX.label,
            position = RoomPoint(0.64f, 0.58f)
        ),
        RoomObjectUi(
            id = "food_bag",
            type = RoomObjectType.FOOD_BAG,
            label = RoomObjectType.FOOD_BAG.label,
            position = RoomPoint(0.28f, 0.38f)
        ),
        RoomObjectUi(
            id = "window",
            type = RoomObjectType.WINDOW,
            label = RoomObjectType.WINDOW.label,
            position = RoomPoint(0.73f, 0.17f)
        )
    )
}

fun initialRoomUiState(): RoomUiState {
    return RoomUiState(
        roomObjects = defaultRoomObjects(),
        pet = defaultPetUiState(),
        statusText = PetAction.IDLE.label
    )
}
