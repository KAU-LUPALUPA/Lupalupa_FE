package com.example.lupapj.data.remote.pet

import com.example.lupapj.data.model.scene.HouseSceneState
import com.example.lupapj.data.model.scene.RoomSceneId
import com.example.lupapj.data.repository.PetRepository

class RemotePetRepository(
    private val apiClient: PetApiClient
) : PetRepository {

    override suspend fun getMyPet(sceneId: RoomSceneId): HouseSceneState {
        return apiClient.getMyPet().toHouseSceneState(sceneId)
    }

    override suspend fun feedPet(petId: String, sceneId: RoomSceneId): HouseSceneState {
        return apiClient.feedPet(petId).toHouseSceneState(sceneId)
    }

    override suspend fun playPet(petId: String, sceneId: RoomSceneId): HouseSceneState {
        return apiClient.playPet(petId).toHouseSceneState(sceneId)
    }

    override suspend fun sleepPet(petId: String, sceneId: RoomSceneId): HouseSceneState {
        return apiClient.sleepPet(petId).toHouseSceneState(sceneId)
    }

    override suspend fun cleanPet(petId: String, sceneId: RoomSceneId): HouseSceneState {
        return apiClient.cleanPet(petId).toHouseSceneState(sceneId)
    }
}
