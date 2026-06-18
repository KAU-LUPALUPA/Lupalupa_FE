package com.example.lupapj.data.repository

import com.example.lupapj.data.model.scene.HouseSceneState
import com.example.lupapj.data.model.scene.RoomSceneId

interface PetRepository {
    suspend fun getMyPet(sceneId: RoomSceneId): HouseSceneState
    suspend fun feedPet(petId: String, sceneId: RoomSceneId): HouseSceneState
    suspend fun playPet(petId: String, sceneId: RoomSceneId): HouseSceneState
    suspend fun sleepPet(petId: String, sceneId: RoomSceneId): HouseSceneState
    suspend fun cleanPet(petId: String, sceneId: RoomSceneId): HouseSceneState
    suspend fun syncPetStatus(petId: String, sceneId: RoomSceneId, request: com.example.lupapj.data.remote.pet.PetStatusSyncRequestDto): HouseSceneState
    suspend fun updateTraitsDebug(petId: String, sceneId: RoomSceneId, request: com.example.lupapj.data.remote.pet.PetTraitsDto): HouseSceneState
}
