package com.example.lupapj.data.model.scene

import com.example.lupapj.data.model.PetAction

data class PetSceneState(
    val action: PetAction,
    val anchor: FloorAnchor
    // TODO: add facing/targetAnchor when movement interpolation is introduced.
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
    petAnchor: FloorAnchor = FloorAnchor(u = 0.44f, v = 0.64f),
    petAction: PetAction = PetAction.IDLE
): HouseSceneState {
    return HouseSceneState(
        currentSceneId = sceneId,
        pet = PetSceneState(
            action = petAction,
            anchor = petAnchor
        ),
        currentSceneRuntime = RoomSceneRuntimeState(sceneId = sceneId)
    )
}
