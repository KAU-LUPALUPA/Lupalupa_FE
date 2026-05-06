package com.example.lupapj.data.remote.room

import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.initialRoomUiState
import com.example.lupapj.data.model.scene.RoomSceneDefinition
import com.example.lupapj.data.remote.pet.PetDto
import com.example.lupapj.data.remote.pet.toHouseSceneState

internal fun RoomLayoutDto.toDomainRoomState(
    sceneResolver: (String) -> RoomSceneDefinition,
    pet: PetDto? = null
): RoomUiState {
    val sceneDefinition = sceneResolver(sceneId)
    val houseSceneState = pet?.toHouseSceneState(sceneDefinition.id)

    return initialRoomUiState(
        sceneDefinition = sceneDefinition,
        houseSceneState = houseSceneState ?: com.example.lupapj.data.model.scene.initialHouseSceneState(
            sceneDefinition.id
        )
    )
}
