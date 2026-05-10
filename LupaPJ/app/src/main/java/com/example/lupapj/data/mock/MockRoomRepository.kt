package com.example.lupapj.data.mock

import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.PetAppearance
import com.example.lupapj.data.model.PetPersonality
import com.example.lupapj.data.model.PetStatus
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.initialRoomUiState
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.HouseSceneState
import com.example.lupapj.data.model.scene.RoomSceneRuntimeState
import com.example.lupapj.data.model.scene.SceneObjectDefinition
import com.example.lupapj.data.model.scene.initialHouseSceneState
import com.example.lupapj.data.repository.RoomRepository
import com.example.lupapj.data.model.scene.updateCurrentSceneRuntime
import com.example.lupapj.data.model.scene.updatePet
import com.example.lupapj.data.model.scene.updatePetStatus
import kotlinx.coroutines.delay

class MockRoomRepository(
    private val localCache: com.example.lupapj.data.local.RoomLocalCache
) : RoomRepository {
    private var isInitialized = false

    private var roomState = initialRoomUiState(
        sceneDefinition = DemoScenes.mainRoom,
        houseSceneState = initialHouseSceneState(
            sceneId = DemoScenes.mainRoom.id,
            petAppearance = PetAppearance(
                headSizeScale = 1.08f,
                bodySizeScale = 0.96f,
                eyeSizeScale = 1.12f,
                noseSizeScale = 0.92f,
                mouthSizeScale = 1.04f
            ),
            petStatus = PetStatus(
                satiety = 80,
                vitality = 75,
                isEgg = false
            ),
            petPersonality = PetPersonality.ACTIVE,
            equippedItemIds = emptyList()
        )
    )

    override suspend fun getRoom(): RoomUiState {
        if (!isInitialized) {
            val food = localCache.getDroppedFood()
            val toy = localCache.getDroppedToy()
            roomState = roomState.copy(
                houseSceneState = roomState.houseSceneState.updateCurrentSceneRuntime {
                    it.copy(
                        droppedFoodAnchor = food?.let { FloorAnchor(it.first, it.second) },
                        droppedToyAnchor = toy?.let { FloorAnchor(it.first, it.second) },
                        isToyKnockedOver = toy?.third ?: false
                    )
                }
            )
            isInitialized = true
        }
        delay(180)
        return roomState
    }

    override suspend fun performObjectAction(objectType: RoomObjectType): RoomUiState {
        delay(220)

        roomState = when (objectType) {
            RoomObjectType.BED -> roomState.copy(
                feedMode = false,
                toyMode = false,
                houseSceneState = roomState.houseSceneState.updateCurrentSceneRuntime {
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

        // [수정됨(권)] 사료 배치 시 펫이 즉시 순간이동하지 않도록 로컬 펫 상태 업데이트 로직 제거
        roomState = roomState.copy(
            feedMode = false,
            toyMode = false,
            houseSceneState = roomState.houseSceneState
                .updateCurrentSceneRuntime {
                    it.copy(droppedFoodAnchor = clampedAnchor)
                }
        )
        localCache.saveDroppedFood(clampedAnchor.u, clampedAnchor.v)

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
        localCache.saveDroppedFood(null, null)

        return roomState
    }

    override suspend fun placeToy(position: FloorAnchor): RoomUiState {
        delay(220)

        if (!roomState.toyMode) return roomState

        val clampedAnchor = FloorAnchor(
            u = position.u.coerceIn(0.16f, 0.84f),
            v = position.v.coerceIn(0.28f, 0.84f)
        )

        // [수정됨(권)] 장난감 배치 시 펫이 즉시 순간이동하지 않도록 로컬 펫 상태 업데이트 로직 제거
        roomState = roomState.copy(
            feedMode = false,
            toyMode = false,
            houseSceneState = roomState.houseSceneState
                .updateCurrentSceneRuntime {
                    it.copy(
                        droppedToyAnchor = clampedAnchor,
                        isToyKnockedOver = false // 새로 놓을 때는 다시 똑바로 세움
                    )
                }
        )
        localCache.saveDroppedToy(clampedAnchor.u, clampedAnchor.v, isKnockedOver = false)

        return roomState
    }

    override suspend fun saveRoomLayout(
        room: RoomUiState
    ): RoomUiState {
        delay(120)

        roomState = room

        return roomState
    }
    override suspend fun updateToyKnockedOver(isKnockedOver: Boolean): RoomUiState {
        roomState = roomState.copy(
            houseSceneState = roomState.houseSceneState.updateCurrentSceneRuntime {
                it.copy(isToyKnockedOver = isKnockedOver)
            }
        )
        localCache.saveToyKnockedOver(isKnockedOver)
        return roomState
    }
}

private fun HouseSceneState.updatePet(
    action: PetAction,
    anchor: FloorAnchor
): HouseSceneState {
    return copy(
        pet = pet.copy(
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
    val objectDefinition = sceneDefinition.objects.firstOrNull {
        it.type == objectType
    }

    return objectDefinition.toFloorAnchorOrNull() ?: fallback
}

private fun SceneObjectDefinition?.toFloorAnchorOrNull(): FloorAnchor? {
    return this?.anchor as? FloorAnchor
}
