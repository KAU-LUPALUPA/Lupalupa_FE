package com.example.lupapj.data.remote.room

interface RoomLayoutApiClient {
    suspend fun validateRoomLayout(
        request: ValidateRoomLayoutRequestDto
    ): RoomLayoutValidationResponseDto

    suspend fun getRoomLayout(): RoomLayoutResponseDto

    suspend fun saveRoomLayout(
        request: SaveRoomLayoutRequestDto
    ): RoomLayoutResponseDto
}

class RoomLayoutApiException(
    val code: String,
    val httpStatus: Int? = null,
    override val message: String? = null
) : Exception(message)
