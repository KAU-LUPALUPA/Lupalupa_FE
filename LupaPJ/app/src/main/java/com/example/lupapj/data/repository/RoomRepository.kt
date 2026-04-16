package com.example.lupapj.data.repository

import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomUiState
import com.example.lupapj.data.model.scene.FloorAnchor

interface RoomRepository {
    suspend fun getRoom(): RoomUiState
    suspend fun performObjectAction(objectType: RoomObjectType): RoomUiState
    suspend fun placeFood(position: FloorAnchor): RoomUiState
    suspend fun consumeFood(): RoomUiState
    suspend fun placeToy(position: FloorAnchor): RoomUiState
}
