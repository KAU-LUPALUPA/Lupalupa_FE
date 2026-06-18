package com.example.lupapj.data.repository

import com.example.lupapj.data.model.scene.HouseSceneState
import com.example.lupapj.data.model.scene.RoomSceneId

interface PetRepository {
    suspend fun getMyPet(sceneId: RoomSceneId): HouseSceneState
    suspend fun feedPet(petId: String, sceneId: RoomSceneId): HouseSceneState
    suspend fun playPet(petId: String, sceneId: RoomSceneId): HouseSceneState
    suspend fun sleepPet(petId: String, sceneId: RoomSceneId): HouseSceneState
    suspend fun cleanPet(petId: String, sceneId: RoomSceneId): HouseSceneState
}
