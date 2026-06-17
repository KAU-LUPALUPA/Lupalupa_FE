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

// [추가됨(권)] 행동 디버깅용 정보
data class BehaviorDebugInfo(
    val consecutiveTicks: Int = 0,
    val currentProbability: Float = 0f,
    val isCrisis: Boolean = false,
    val isVisible: Boolean = false,
    val mValue: Float = 0f,
    val kValue: Float = 0f
)

enum class AppPhase {
    LOGIN_PROMPT,
    SPLASH_LOADING,
    START_PROMPT,
    ROOM,
    GALLERY, // [추가됨] 갤러리 화면 상태 추가
    FRIENDS,
    FRIEND_ROOM,
    PLAZA,
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
    WALKING,
    RESTING,
    BED_RESTING, // [추가됨(권)] 침대 휴식
    PLAYING,
    EATING,
    CLEANING,
    GROOM,       // [추가됨(V2)] 몸단장
    TIDY,        // [추가됨(V2)] 장난감 정리
    STRETCH      // [추가됨(V2)] 기지개
}

// [추가됨(V2)] 연속형 성격 특성
data class PetTraits(
    val activity: Float = 0.5f,
    val appetite: Float = 0.5f,
    val attention: Float = 0.5f,
    val curiosity: Float = 0.5f,
    val patience: Float = 0.5f
)

// [추가됨(V2)] 내부 렌더링용 파생 특성
data class DerivedTraits(
    val vigor: Float = 0.5f,
    val volatility: Float = 0.5f,
    val tidiness: Float = 0.5f,
    val boldness: Float = 0.5f,
    val restfulness: Float = 0.5f,
    val distractibility: Float = 0.5f
)

// [추가됨(V2)] 실시간 감정 상태
data class AffectState(
    val valence: Float = 0f,
    val arousal: Float = 0f,
    val moodLabel: String = "NORMAL"
)

const val DEFAULT_PET_ID = "pet_local"
const val DEFAULT_PET_OWNER_USER_ID = "user_local"
const val DEFAULT_PET_NAME = "루파"
const val DEFAULT_PET_CHARACTER_ASSET_KEY = "room/characters/lupa_default"

data class PetAppearance(
    val headSizeScale: Float = 1f,
    val bodySizeScale: Float = 1f,
    val eyeSizeScale: Float = 1f,
    val noseSizeScale: Float = 1f,
    val mouthSizeScale: Float = 1f
)

data class PetStatus(
    val satiety: Int = 100, // [수정됨(권)] 서버 초기값과 일치
    val vitality: Int = 100, // [수정됨(권)] 서버 초기값과 일치
    val cleanliness: Int = 100, // [추가됨(V2)] 청결도
    val isEgg: Boolean = false
)

data class PetUiState(
    val petId: String = DEFAULT_PET_ID,
    val ownerUserId: String = DEFAULT_PET_OWNER_USER_ID,
    val name: String = DEFAULT_PET_NAME,
    val characterAssetKey: String = DEFAULT_PET_CHARACTER_ASSET_KEY,
    val appearance: PetAppearance = PetAppearance(),
    val status: PetStatus = PetStatus(),
    val traits: PetTraits = PetTraits(),
    val affect: AffectState = AffectState(),
    val equippedItemIds: List<String> = emptyList(),
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
    val cameraZoom: Float = 1f, // [추가됨] 카메라 줌 배율 (기본 1x)
    val cameraOffsetX: Float = 0f, // [추가됨] 카메라 모드 X축 이동 (Panning)
    val cameraOffsetY: Float = 0f, // [추가됨] 카메라 모드 Y축 이동 (Panning)

    val rearrangeMode: Boolean = false,

    val selectedRearrangeObjectType: RoomObjectType? = null,
    val layoutRevision: Int? = null,
    val layoutHash: String? = null,
    val layoutUpdatedAt: String? = null
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
            petId = houseSceneState.pet.petId,
            ownerUserId = houseSceneState.pet.ownerUserId,
            name = houseSceneState.pet.name,
            characterAssetKey = houseSceneState.pet.characterAssetKey,
            appearance = houseSceneState.pet.appearance,
            status = houseSceneState.pet.status,
            traits = houseSceneState.pet.traits,
            affect = houseSceneState.pet.affect,
            equippedItemIds = houseSceneState.pet.equippedItemIds,
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

enum class MainMenuAction {
    SCREENSHOT,
    GALLERY,
    CONTACTS,
    SHOP,
    PLAYGROUND,
    MINIGAME // [수정됨(권)] 최근 활동을 위해 미니게임 액션 추가
}

val PetAction.label: String
    get() = when (this) {
        PetAction.IDLE -> "대기 중"
        PetAction.WALKING -> "산책 중"
        PetAction.RESTING -> "휴식 중"
        PetAction.BED_RESTING -> "침대에서 휴식 중" // [추가됨(권)]
        PetAction.PLAYING -> "노는 중"
        PetAction.EATING -> "먹는 중"
        PetAction.CLEANING -> "청소 중"
        PetAction.GROOM -> "몸단장 중"
        PetAction.TIDY -> "정리 중"
        PetAction.STRETCH -> "기지개 켜는 중"
    }

val BottomNavItem.label: String
    get() = when (this) {
        BottomNavItem.SHOP -> "상점"
        BottomNavItem.SCREENSHOT -> "스크린샷"
        BottomNavItem.CONTACTS -> "친구"
        BottomNavItem.GALLERY -> "갤러리"
    }

val MainMenuAction.label: String
    get() = when (this) {
        MainMenuAction.SCREENSHOT -> "스크린샷"
        MainMenuAction.GALLERY -> "갤러리"
        MainMenuAction.CONTACTS -> "친구"
        MainMenuAction.SHOP -> "상점"
        MainMenuAction.PLAYGROUND -> "광장"
        MainMenuAction.MINIGAME -> "미니게임" // [수정됨(권)]
    }

// [수정됨(권)] 최근 활동 아이콘 매핑
val MainMenuAction.iconRes: Int
    get() = when (this) {
        MainMenuAction.SCREENSHOT -> com.example.lupapj.R.drawable.camera_trimmed
        MainMenuAction.GALLERY -> com.example.lupapj.R.drawable.gallery_trimmed
        MainMenuAction.CONTACTS -> com.example.lupapj.R.drawable.friends_trimmed
        MainMenuAction.SHOP -> com.example.lupapj.R.drawable.shop_trimmed
        MainMenuAction.PLAYGROUND -> com.example.lupapj.R.drawable.playground_trimmed
        MainMenuAction.MINIGAME -> com.example.lupapj.R.drawable.minigame_icon // [수정됨(권)] 전용 아이콘 사용
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
    val timestamp: Long,
    val isBackedUp: Boolean = false, // [추가됨] 백업 상태 (구름 아이콘)
    val serverImageId: String? = null // [추가됨] 서버 동기화(삭제/즐겨찾기)용 ID
)

// [추가됨(권)] 재화 상태 모델. 서버 스펙(Long)에 맞춰 타입을 변경했습니다.
data class CurrencyState(
    val amount: Long = 0L
)

// [추가됨(권)] 상점 아이템의 카테고리 (치장 부위 구분을 위해 사용)
// 대분류 10(치장) 하위의 중분류(부위) 코드와 매칭됩니다.
enum class ShopCategory(val code: String) {
    HAT("01"),
    FACE_DECOR("02"),
    TOP("03"),
    BOTTOM("04"),
    FULL_BODY("05"),
    SHOES("06")
}

// [추가됨(권)] 상점 카테고리를 UI에 표시하기 위한 한국어 라벨 확장 프로퍼티
val ShopCategory.label: String
    get() = when (this) {
        ShopCategory.HAT -> "모자"
        ShopCategory.FACE_DECOR -> "얼굴 치장"
        ShopCategory.TOP -> "상의"
        ShopCategory.BOTTOM -> "하의"
        ShopCategory.FULL_BODY -> "전신 옷"
        ShopCategory.SHOES -> "신발"
    }

// [추가됨(권)] 상점 아이템 모델. 구매 시 필요한 재화(price) 및 미리보기 오버레이 리소스(previewOverlayResId) 포함.
// [수정됨(권)] UI 로직 하드코딩 방지를 위한 에셋 렌더링 속성(scale, offset, aspect ratio) 추가
data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val category: ShopCategory,
    val thumbnailResId: Int? = null,
    val previewOverlayResId: Int? = null,
    val overlayScale: Float = 1.0f,
    val overlayOffsetYRatio: Float = 0f,
    val overlayAspectRatio: Float? = null // 지정되지 않으면 베이스 스프라이트 캔버스에 1:1 매칭(fillMaxSize)
)

/**
 * [추가됨(권)] 인벤토리 아이템 모델.
 * 서버에서 발급한 고유 인스턴스 ID와 마스터 데이터 ID를 연결하며, 수량(count) 정보를 포함합니다.
 */
data class InventoryItem(
    val instanceId: String, // uuid + 랜덤숫자 10자리
    val masterId: String,   // Master Data ID (8-digit)
    val count: Int = 1      // [추가됨] 아이템 보유 수량
)

// [추가됨(권)] 앱 내에 내장된 상점 마스터 데이터 (로컬 기본값)
val DefaultShopItems = listOf(
    // 모자 (01)
    // [수정됨(권)] 밀짚모자 에셋 설정 (이미지 및 미리보기 오버레이) 및 렌더링 파라미터 분리
    ShopItem(
        id = "10010001",
        name = "밀짚모자",
        description = "여름에 쓰기 좋은 시원한 모자입니다.",
        price = 100,
        category = ShopCategory.HAT,
        thumbnailResId = com.example.lupapj.R.drawable.straw_hat,
        previewOverlayResId = com.example.lupapj.R.drawable.straw_hat,
        overlayScale = 1.15f,
        overlayOffsetYRatio = 0.45f,
        overlayAspectRatio = 481f / 519f
    ),
    ShopItem("10010002", "캡모자", "활동적인 느낌의 깔끔한 캡모자입니다.", 200, ShopCategory.HAT),
    
    // 얼굴 치장 (02)
    ShopItem("10020001", "선글라스", "멋진 검은색 선글라스입니다.", 300, ShopCategory.FACE_DECOR),
    
    // 상의 (03)
    ShopItem("10030001", "무지 반팔 티셔츠", "어디에나 잘 어울리는 기본 티셔츠입니다.", 400, ShopCategory.TOP),
    ShopItem("10030002", "파자마 셔츠", "편안한 잠을 위한 파자마 상의입니다.", 500, ShopCategory.TOP),
    
    // 하의 (04)
    ShopItem("10040001", "기본 면바지", "깔끔한 핏의 기본 면바지입니다.", 600, ShopCategory.BOTTOM),
    ShopItem("10040002", "파자마 바지", "편안한 잠을 위한 파자마 하의입니다.", 700, ShopCategory.BOTTOM),
    
    // 전신 옷 (05)
    ShopItem("10050001", "공룡 잠옷(전신)", "귀여운 공룡 모양의 전신 잠옷입니다.", 800, ShopCategory.FULL_BODY),
    ShopItem("10050002", "원피스", "화사한 느낌의 예쁜 원피스입니다.", 900, ShopCategory.FULL_BODY),
    
    // 신발 (06)
    ShopItem("10060001", "파란 신발", "가볍게 뛰어다니기 좋은 파란 신발입니다.", 1000, ShopCategory.SHOES),
    ShopItem("10060002", "빨간 신발", "열정적인 빨간색이 돋보이는 신발입니다.", 1100, ShopCategory.SHOES)
)
