package com.example.lupapj.data.mock

import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.initialRoomUiState
import com.example.lupapj.data.repository.RoomRepository
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.HouseSceneState
import com.example.lupapj.data.model.scene.PetSceneState
import com.example.lupapj.data.model.scene.RoomSceneRuntimeState
import com.example.lupapj.data.model.scene.SceneObjectDefinition
import kotlinx.coroutines.delay

class MockRoomRepository : RoomRepository {
    private var roomState = initialRoomUiState(
        sceneDefinition = DemoScenes.mainRoom
    )

    override suspend fun getRoom(): RoomUiState {
        delay(180)
        return roomState
    }

    override suspend fun performObjectAction(objectType: RoomObjectType): RoomUiState {
        delay(220)
        roomState = when (objectType) {
            RoomObjectType.BED -> roomState.copy(
                feedMode = false,
                toyMode = false,
                houseSceneState = roomState.houseSceneState.updatePet(
                    action = PetAction.RESTING,
                    anchor = roomState.resolveFloorObjectAnchor(
                        objectType = RoomObjectType.BED,
                        fallback = FloorAnchor(u = 0.33f, v = 0.62f)
                    )
                ).updateCurrentSceneRuntime {
                    it.copy(
                        droppedFoodAnchor = null,
                        droppedToyAnchor = null
                    )
                }
            )

            RoomObjectType.TOY_BOX -> roomState.copy(
                feedMode = false,
                toyMode = true
            )

            RoomObjectType.FOOD_BAG -> roomState.copy(
                feedMode = true,
                toyMode = false
            )

            RoomObjectType.WINDOW -> roomState
        }
        return roomState
    }

    override suspend fun placeFood(position: FloorAnchor): RoomUiState {
        delay(220)
        if (!roomState.feedMode) return roomState

        val clampedAnchor = FloorAnchor(
            u = position.u.coerceIn(0.16f, 0.84f),
            v = position.v.coerceIn(0.28f, 0.84f)
        )

        roomState = roomState.copy(
            feedMode = false,
            toyMode = false,
            houseSceneState = roomState.houseSceneState
                .updateCurrentSceneRuntime {
                    it.copy(droppedFoodAnchor = clampedAnchor)
                }
                .updatePet(
                    action = PetAction.EATING,
                    anchor = FloorAnchor(
                        u = clampedAnchor.u,
                        v = (clampedAnchor.v - 0.05f).coerceAtLeast(0.22f)
                    )
                )
        )
        return roomState
    }

    override suspend fun consumeFood(): RoomUiState {
        delay(120)

        if (roomState.houseSceneState.currentSceneRuntime.droppedFoodAnchor == null) {
            return roomState
        }

        roomState = roomState.copy(
            houseSceneState = roomState.houseSceneState
                .updateCurrentSceneRuntime {
                    it.copy(droppedFoodAnchor = null)
                }
                .updatePetActionIf(
                    current = PetAction.EATING,
                    next = PetAction.IDLE
                )
        )
        return roomState
    }

    override suspend fun placeToy(position: FloorAnchor): RoomUiState {
        delay(220)
        if (!roomState.toyMode) return roomState

        val clampedAnchor = FloorAnchor(
            u = position.u.coerceIn(0.16f, 0.84f),
            v = position.v.coerceIn(0.28f, 0.84f)
        )

        roomState = roomState.copy(
            feedMode = false,
            toyMode = false,
            houseSceneState = roomState.houseSceneState
                .updateCurrentSceneRuntime {
                    it.copy(droppedToyAnchor = clampedAnchor)
                }
                .updatePet(
                    action = PetAction.PLAYING,
                    anchor = FloorAnchor(
                        u = clampedAnchor.u,
                        v = (clampedAnchor.v - 0.04f).coerceAtLeast(0.22f)
                    )
                )
        )
        return roomState
    }
}

private fun HouseSceneState.updatePet(
    action: PetAction,
    anchor: FloorAnchor
): HouseSceneState {
    return copy(
        pet = PetSceneState(
            action = action,
            anchor = anchor
        )
    )
}

private fun HouseSceneState.updateCurrentSceneRuntime(
    transform: (RoomSceneRuntimeState) -> RoomSceneRuntimeState
): HouseSceneState {
    return copy(
        currentSceneRuntime = transform(currentSceneRuntime)
    )
}

private fun HouseSceneState.updatePetActionIf(
    current: PetAction,
    next: PetAction
): HouseSceneState {
    if (pet.action != current) return this
    return copy(
        pet = pet.copy(action = next)
    )
}

private fun RoomUiState.resolveFloorObjectAnchor(
    objectType: RoomObjectType,
    fallback: FloorAnchor
): FloorAnchor {
    val objectDefinition = sceneDefinition.objects.firstOrNull { it.type == objectType }
    return objectDefinition.toFloorAnchorOrNull() ?: fallback
}

private fun SceneObjectDefinition?.toFloorAnchorOrNull(): FloorAnchor? {
    return this?.anchor as? FloorAnchor
}
