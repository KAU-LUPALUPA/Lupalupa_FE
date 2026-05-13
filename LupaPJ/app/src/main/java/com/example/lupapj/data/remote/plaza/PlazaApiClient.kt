package com.example.lupapj.data.remote.plaza

interface PlazaApiClient {
    suspend fun joinRandomPlaza(): PlazaRoomEnvelopeDto

    suspend fun joinPlazaByCode(request: JoinPlazaByCodeRequestDto): PlazaRoomEnvelopeDto

    suspend fun getMyActivePlaza(): PlazaRoomEnvelopeDto

    suspend fun getPlazaSnapshot(plazaId: String): PlazaRoomEnvelopeDto

    suspend fun leavePlaza(plazaId: String)

    suspend fun sendMessage(
        plazaId: String,
        request: SendPlazaMessageRequestDto
    ): PlazaChatMessageEnvelopeDto
}

class PlazaApiException(
    val code: String,
    val httpStatus: Int? = null,
    override val message: String? = null
) : Exception(message)
