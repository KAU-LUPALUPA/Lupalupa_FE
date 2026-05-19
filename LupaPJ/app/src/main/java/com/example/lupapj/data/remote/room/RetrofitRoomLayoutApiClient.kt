package com.example.lupapj.data.remote.room

import com.google.gson.Gson
import retrofit2.Response

class RetrofitRoomLayoutApiClient(
    private val service: RoomRetrofitService,
    private val gson: Gson = Gson()
) : RoomLayoutApiClient {
    override suspend fun validateRoomLayout(
        request: ValidateRoomLayoutRequestDto
    ): RoomLayoutValidationResponseDto {
        return service.validateRoomLayout(request).bodyOrThrow()
    }

    override suspend fun getRoomLayout(): RoomLayoutResponseDto {
        return service.getRoomLayout().bodyOrThrow()
    }

    override suspend fun saveRoomLayout(
        request: SaveRoomLayoutRequestDto
    ): RoomLayoutResponseDto {
        return service.saveRoomLayout(request).bodyOrThrow()
    }

    private fun <T> Response<T>.bodyOrThrow(): T {
        if (!isSuccessful) throw toRoomLayoutApiException()
        return body() ?: throw RoomLayoutApiException(
            code = "EMPTY_RESPONSE",
            httpStatus = code(),
            message = "Response body is empty."
        )
    }

    private fun Response<*>.toRoomLayoutApiException(): RoomLayoutApiException {
        val parsedError = errorBody()?.string()?.let { raw ->
            runCatching {
                gson.fromJson(raw, ErrorResponseDto::class.java)
            }.getOrNull()
        }
        return RoomLayoutApiException(
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
