package com.example.lupapj.data.remote.plaza

import com.google.gson.Gson
import retrofit2.Response

class RetrofitPlazaApiClient(
    private val service: PlazaRetrofitService,
    private val gson: Gson = Gson()
) : PlazaApiClient {
    override suspend fun joinRandomPlaza(): PlazaRoomEnvelopeDto {
        return service.joinRandomPlaza().bodyOrThrow()
    }

    override suspend fun joinPlazaByCode(
        request: JoinPlazaByCodeRequestDto
    ): PlazaRoomEnvelopeDto {
        return service.joinPlazaByCode(request).bodyOrThrow()
    }

    override suspend fun getMyActivePlaza(): PlazaRoomEnvelopeDto {
        return service.getMyActivePlaza().bodyOrThrow()
    }

    override suspend fun getPlazaSnapshot(plazaId: String): PlazaRoomEnvelopeDto {
        return service.getPlazaSnapshot(plazaId).bodyOrThrow()
    }

    override suspend fun leavePlaza(plazaId: String) {
        service.leavePlaza(plazaId).throwIfFailed()
    }

    override suspend fun sendMessage(
        plazaId: String,
        request: SendPlazaMessageRequestDto
    ): PlazaChatMessageEnvelopeDto {
        return service.sendMessage(plazaId, request).bodyOrThrow()
    }

    private fun <T> Response<T>.bodyOrThrow(): T {
        if (!isSuccessful) throw toPlazaApiException()
        return body() ?: throw PlazaApiException(
            code = "EMPTY_RESPONSE",
            httpStatus = code(),
            message = "Response body is empty."
        )
    }

    private fun Response<*>.throwIfFailed() {
        if (!isSuccessful) throw toPlazaApiException()
    }

    private fun Response<*>.toPlazaApiException(): PlazaApiException {
        val parsedError = errorBody()?.string()?.let { raw ->
            runCatching {
                gson.fromJson(raw, ErrorResponseDto::class.java)
            }.getOrNull()
        }
        return PlazaApiException(
            code = parsedError?.code ?: "HTTP_${code()}",
            httpStatus = code(),
            message = parsedError?.message ?: message()
        )
    }

    private data class ErrorResponseDto(
        val code: String? = null,
        val message: String? = null
    )
}
