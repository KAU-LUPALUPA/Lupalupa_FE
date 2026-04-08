package com.example.lupapj.data.repository

import com.example.lupapj.data.model.RoomObjectType
import com.example.lupapj.data.model.RoomPoint
import com.example.lupapj.data.model.RoomUiState

interface RoomRepository {
    suspend fun getRoom(): RoomUiState
    suspend fun performObjectAction(objectType: RoomObjectType): RoomUiState
    suspend fun placeFood(position: RoomPoint): RoomUiState
}
