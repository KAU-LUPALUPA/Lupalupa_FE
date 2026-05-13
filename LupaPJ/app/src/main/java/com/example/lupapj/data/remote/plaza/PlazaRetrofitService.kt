package com.example.lupapj.data.remote.plaza

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PlazaRetrofitService {
    @POST("plazas/random/join")
    suspend fun joinRandomPlaza(
        @Body request: EmptyPlazaRequestDto = EmptyPlazaRequestDto()
    ): Response<PlazaRoomEnvelopeDto>

    @POST("plazas/code/join")
    suspend fun joinPlazaByCode(
        @Body request: JoinPlazaByCodeRequestDto
    ): Response<PlazaRoomEnvelopeDto>

    @GET("plazas/me/active")
    suspend fun getMyActivePlaza(): Response<PlazaRoomEnvelopeDto>

    @GET("plazas/{plazaId}")
    suspend fun getPlazaSnapshot(
        @Path("plazaId") plazaId: String
    ): Response<PlazaRoomEnvelopeDto>

    @POST("plazas/{plazaId}/leave")
    suspend fun leavePlaza(
        @Path("plazaId") plazaId: String
    ): Response<Unit>

    @POST("plazas/{plazaId}/messages")
    suspend fun sendMessage(
        @Path("plazaId") plazaId: String,
        @Body request: SendPlazaMessageRequestDto
    ): Response<PlazaChatMessageEnvelopeDto>
}
