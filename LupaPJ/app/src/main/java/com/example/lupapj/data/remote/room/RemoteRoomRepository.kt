package com.example.lupapj.data.remote.room

import com.example.lupapj.data.local.RoomCache
import com.example.lupapj.data.local.RoomLayoutSnapshot
import com.example.lupapj.data.model.PetAction
import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.initialRoomUiState
import com.example.lupapj.data.model.scene.FloorAnchor
import com.example.lupapj.data.model.scene.RoomSceneDefinition
import com.example.lupapj.data.model.scene.updateCurrentSceneRuntime
import com.example.lupapj.data.repository.RoomRepository
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException

class RemoteRoomRepository(
    private val apiClient: RoomLayoutApiClient,
    private val localCache: RoomCache,
    private val sceneResolver: (String) -> RoomSceneDefinition,
    private val gson: Gson = Gson()
) : RoomRepository {
    private var roomState = initialRoomUiState(
        sceneDefinition = sceneResolver(DEFAULT_SCENE_ID)
    )

    override suspend fun getRoom(): RoomUiState {
        roomState = loadValidatedRemoteLayoutOrCurrent(roomState)
            .withLocalRuntimeCache()
        return roomState
    }

    override suspend fun refreshRoomLayout(): RoomUiState {
        roomState = loadRemoteLayout(roomState)
            .withLocalRuntimeCache()
        return roomState
    }

    override suspend fun performObjectAction(objectType: RoomObjectType): RoomUiState {
        roomState = when (objectType) {
            RoomObjectType.BED -> {
                localCache.saveDroppedFood(null, null)
                localCache.saveDroppedToy(null, null)
                roomState.copy(
                    feedMode = false,
                    toyMode = false,
                    houseSceneState = roomState.houseSceneState.updateCurrentSceneRuntime {
                        it.copy(
                            droppedFoodAnchor = null,
                            droppedToyAnchor = null,
                            isToyKnockedOver = false
                        )
                    }
                )
            }

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
        if (!roomState.feedMode) return roomState

        val clampedAnchor = FloorAnchor(
            u = position.u.coerceIn(0.16f, 0.84f),
            v = position.v.coerceIn(0.28f, 0.84f)
        )

        roomState = roomState.copy(
            feedMode = false,
            toyMode = false,
            houseSceneState = roomState.houseSceneState.updateCurrentSceneRuntime {
                it.copy(droppedFoodAnchor = clampedAnchor)
            }
        )
        localCache.saveDroppedFood(clampedAnchor.u, clampedAnchor.v)

        return roomState
    }

    override suspend fun consumeFood(): RoomUiState {
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
        if (!roomState.toyMode) return roomState

        val clampedAnchor = FloorAnchor(
            u = position.u.coerceIn(0.16f, 0.84f),
            v = position.v.coerceIn(0.28f, 0.84f)
        )

        roomState = roomState.copy(
            feedMode = false,
            toyMode = false,
            houseSceneState = roomState.houseSceneState.updateCurrentSceneRuntime {
                it.copy(
                    droppedToyAnchor = clampedAnchor,
                    isToyKnockedOver = false
                )
            }
        )
        localCache.saveDroppedToy(clampedAnchor.u, clampedAnchor.v, isKnockedOver = false)

        return roomState
    }

    override suspend fun saveRoomLayout(room: RoomUiState): RoomUiState {
        return try {
            val savedLayout = apiClient
                .saveRoomLayout(room.toSaveRoomLayoutRequestDto())
                .roomLayout

            cacheRoomLayout(savedLayout)
            roomState = savedLayout.toDomainRoomState(
                sceneResolver = sceneResolver,
                fallbackHouseSceneState = room.houseSceneState
            )
            roomState
        } catch (exception: RoomLayoutApiException) {
            throw exception
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            throw exception
        }
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

    override suspend fun cleanupToy(): RoomUiState {
        roomState = roomState.copy(
            houseSceneState = roomState.houseSceneState.updateCurrentSceneRuntime {
                it.copy(
                    droppedToyAnchor = null,
                    isToyKnockedOver = false
                )
            }
        )
        localCache.saveDroppedToy(null, null)
        return roomState
    }

    private suspend fun loadRemoteLayoutOrCurrent(currentRoom: RoomUiState): RoomUiState {
        return try {
            loadRemoteLayout(currentRoom)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            currentRoom
        }
    }

    private suspend fun loadValidatedRemoteLayoutOrCurrent(currentRoom: RoomUiState): RoomUiState {
        val cachedSnapshot = localCache.getRoomLayoutSnapshot()
        val cachedLayout = cachedSnapshot?.toRoomLayoutDtoOrNull()
        return try {
            val serverLayout = if (cachedSnapshot != null) {
                val validation = apiClient.validateRoomLayout(
                    ValidateRoomLayoutRequestDto(
                        localLayoutRevision = cachedSnapshot.layoutRevision,
                        localLayoutHash = cachedSnapshot.layoutHash,
                        localUpdatedAt = cachedSnapshot.updatedAt
                    )
                )
                if (validation.syncStatus == SYNC_STATUS_MATCH && cachedLayout != null) {
                    cachedLayout
                } else {
                    validation.roomLayout ?: apiClient.getRoomLayout().roomLayout
                }
            } else {
                apiClient.getRoomLayout().roomLayout
            }

            cacheRoomLayout(serverLayout)
            serverLayout.toDomainRoomState(
                sceneResolver = sceneResolver,
                fallbackHouseSceneState = currentRoom.houseSceneState
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            cachedLayout?.toDomainRoomState(
                sceneResolver = sceneResolver,
                fallbackHouseSceneState = currentRoom.houseSceneState
            ) ?: currentRoom
        }
    }

    private suspend fun loadRemoteLayout(currentRoom: RoomUiState): RoomUiState {
        val serverLayout = apiClient
            .getRoomLayout()
            .roomLayout
        cacheRoomLayout(serverLayout)
        return serverLayout.toDomainRoomState(
            sceneResolver = sceneResolver,
            fallbackHouseSceneState = currentRoom.houseSceneState
        )
    }

    private suspend fun cacheRoomLayout(roomLayout: RoomLayoutDto) {
        localCache.saveRoomLayoutSnapshot(
            layoutRevision = roomLayout.layoutRevision,
            layoutHash = roomLayout.layoutHash,
            updatedAt = roomLayout.updatedAt,
            layoutJson = gson.toJson(roomLayout)
        )
    }

    private fun RoomLayoutSnapshot.toRoomLayoutDtoOrNull(): RoomLayoutDto? {
        return runCatching {
            gson.fromJson(layoutJson, RoomLayoutDto::class.java)
        }.getOrNull()
    }

    private suspend fun RoomUiState.withLocalRuntimeCache(): RoomUiState {
        val food = localCache.getDroppedFood()
        val toy = localCache.getDroppedToy()
        return copy(
            houseSceneState = houseSceneState.updateCurrentSceneRuntime {
                it.copy(
                    sceneId = sceneDefinition.id,
                    droppedFoodAnchor = food?.let { saved -> FloorAnchor(saved.first, saved.second) },
                    droppedToyAnchor = toy?.let { saved -> FloorAnchor(saved.first, saved.second) },
                    isToyKnockedOver = toy?.third ?: false
                )
            }
        )
    }

    private fun com.example.lupapj.data.model.scene.HouseSceneState.updatePetActionIf(
        current: PetAction,
        next: PetAction
    ): com.example.lupapj.data.model.scene.HouseSceneState {
        if (pet.action != current) return this
        return copy(pet = pet.copy(action = next))
    }

    private companion object {
        const val DEFAULT_SCENE_ID = "main_room"
        const val SYNC_STATUS_MATCH = "MATCH"
    }
}
