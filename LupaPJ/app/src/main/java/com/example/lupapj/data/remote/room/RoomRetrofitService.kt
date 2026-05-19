package com.example.lupapj.data.remote.room

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface RoomRetrofitService {
    @POST("rooms/me/layout/validate")
    suspend fun validateRoomLayout(
        @Body request: ValidateRoomLayoutRequestDto
    ): Response<RoomLayoutValidationResponseDto>

    @GET("rooms/me/layout")
    suspend fun getRoomLayout(): Response<RoomLayoutResponseDto>

    @PUT("rooms/me/layout")
    suspend fun saveRoomLayout(
        @Body request: SaveRoomLayoutRequestDto
    ): Response<RoomLayoutResponseDto>
}
