package com.example.lupapj.data.model.scene

import com.example.lupapj.data.model.DEFAULT_PET_CHARACTER_ASSET_KEY
import com.example.lupapj.data.model.DEFAULT_PET_ID
import com.example.lupapj.data.model.DEFAULT_PET_NAME
import com.example.lupapj.data.model.DEFAULT_PET_OWNER_USER_ID
import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.PetAppearance
import com.example.lupapj.data.model.PetPersonality
import com.example.lupapj.data.model.PetStatus

data class PetSceneState(
    val petId: String = DEFAULT_PET_ID,
    val ownerUserId: String = DEFAULT_PET_OWNER_USER_ID,
    val name: String = DEFAULT_PET_NAME,
    val characterAssetKey: String = DEFAULT_PET_CHARACTER_ASSET_KEY,
    val appearance: PetAppearance = PetAppearance(),
    val status: PetStatus = PetStatus(),
    val personality: PetPersonality = PetPersonality.ACTIVE,
    val equippedItemIds: List<String> = emptyList(),
    val movement: PetMovementState = PetMovementState(),
    val action: PetAction,
    val anchor: FloorAnchor
)

enum class PetMovementStyle {
    SMOOTH,
    BOUNCY
}

data class PetMovementState(
    val targetAnchor: FloorAnchor? = null,
    val isMoving: Boolean = false,
    val style: PetMovementStyle = PetMovementStyle.BOUNCY,
    val isAutonomous: Boolean = false
)

data class RoomSceneRuntimeState(
    val sceneId: RoomSceneId,
    val droppedFoodAnchor: FloorAnchor? = null,
    val droppedToyAnchor: FloorAnchor? = null
    // TODO: add room-local interaction state such as moved objects or temporary blockers.
)

data class HouseSceneState(
    val currentSceneId: RoomSceneId,
    val pet: PetSceneState,
    val currentSceneRuntime: RoomSceneRuntimeState
    // TODO: expand to runtimeByScene: Map<RoomSceneId, RoomSceneRuntimeState> for multi-room persistence.
)

fun initialHouseSceneState(
    sceneId: RoomSceneId,
    petId: String = DEFAULT_PET_ID,
    ownerUserId: String = DEFAULT_PET_OWNER_USER_ID,
    petName: String = DEFAULT_PET_NAME,
    characterAssetKey: String = DEFAULT_PET_CHARACTER_ASSET_KEY,
    petAppearance: PetAppearance = PetAppearance(),
    petStatus: PetStatus = PetStatus(),
    petPersonality: PetPersonality = PetPersonality.ACTIVE,
    equippedItemIds: List<String> = emptyList(),
    petAnchor: FloorAnchor = FloorAnchor(u = 0.44f, v = 0.64f),
    petAction: PetAction = PetAction.IDLE
): HouseSceneState {
    return HouseSceneState(
        currentSceneId = sceneId,
        pet = PetSceneState(
            petId = petId,
            ownerUserId = ownerUserId,
            name = petName,
            characterAssetKey = characterAssetKey,
            appearance = petAppearance,
            status = petStatus,
            personality = petPersonality,
            equippedItemIds = equippedItemIds,
            action = petAction,
            anchor = petAnchor
        ),
        currentSceneRuntime = RoomSceneRuntimeState(sceneId = sceneId)
    )
}
