package com.example.lupapj.data.remote.plaza

import android.util.Log
import com.example.lupapj.data.model.plaza.PLAZA_MESSAGE_MAX_LENGTH
import com.example.lupapj.data.model.plaza.PlazaChatMessage
import com.example.lupapj.data.model.plaza.PlazaCode
import com.example.lupapj.data.model.plaza.PlazaOperationFailure
import com.example.lupapj.data.model.plaza.PlazaOperationResult
import com.example.lupapj.data.model.plaza.PlazaPetSnapshot
import com.example.lupapj.data.model.plaza.PlazaRoom
import com.example.lupapj.data.repository.PlazaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import com.google.gson.JsonParseException

class RemotePlazaRepository(
    private val apiClient: PlazaApiClient
) : PlazaRepository {
    private val _activePlaza = MutableStateFlow<PlazaRoom?>(null)
    override val activePlaza: StateFlow<PlazaRoom?> = _activePlaza.asStateFlow()

    private var currentUserId: String? = null
    private var currentNickname: String? = null

    override fun updateCurrentUser(userId: String?, nickname: String?) {
        currentUserId = userId?.takeIf { it.isNotBlank() } ?: currentUserId
        currentNickname = nickname?.takeIf { it.isNotBlank() } ?: currentNickname
    }

    override suspend fun refreshActivePlaza(): PlazaOperationResult<PlazaRoom?> {
        return apiCall {
            val plaza = apiClient
                .getMyActivePlaza()
                .plaza
                ?.toDomain(
                    currentUserId = currentUserId,
                    currentNickname = currentNickname
                )
            _activePlaza.value = plaza
            plaza
        }
    }

    override suspend fun refreshPlazaSnapshot(plazaId: String): PlazaOperationResult<PlazaRoom> {
        return apiCall {
            val plaza = apiClient
                .getPlazaSnapshot(plazaId)
                .plaza
                ?.toDomain(
                    currentUserId = currentUserId,
                    currentNickname = currentNickname
                )
                ?: throw PlazaApiException(code = "PLAZA_NOT_FOUND", httpStatus = 404)
            _activePlaza.value = plaza
            plaza
        }
    }

    override suspend fun joinRandomPlaza(
        currentUserId: String,
        nickname: String,
        pet: PlazaPetSnapshot
    ): PlazaOperationResult<PlazaRoom> {
        updateCurrentUser(currentUserId, nickname)
        return apiCall {
            val plaza = apiClient
                .joinRandomPlaza()
                .plaza
                ?.toDomain(
                    currentUserId = currentUserId,
                    currentNickname = nickname
                )
                ?: throw PlazaApiException(code = "PLAZA_NOT_FOUND", httpStatus = 404)
            _activePlaza.value = plaza
            plaza
        }
    }

    override suspend fun joinPlazaByCode(
        codeInput: String,
        currentUserId: String,
        nickname: String,
        pet: PlazaPetSnapshot
    ): PlazaOperationResult<PlazaRoom> {
        updateCurrentUser(currentUserId, nickname)
        if (codeInput.isBlank()) {
            return PlazaOperationResult.Failure(PlazaOperationFailure.EMPTY_CODE)
        }
        val code = PlazaCode.fromInput(codeInput)
            ?: return PlazaOperationResult.Failure(PlazaOperationFailure.INVALID_CODE)

        return apiCall {
            val plaza = apiClient
                .joinPlazaByCode(JoinPlazaByCodeRequestDto(code = code.value))
                .plaza
                ?.toDomain(
                    currentUserId = currentUserId,
                    currentNickname = nickname
                )
                ?: throw PlazaApiException(code = "PLAZA_NOT_FOUND", httpStatus = 404)
            _activePlaza.value = plaza
            plaza
        }
    }

    override suspend fun leavePlaza(): PlazaOperationResult<Unit> {
        val plazaId = _activePlaza.value?.plazaId
            ?: return PlazaOperationResult.Failure(PlazaOperationFailure.NOT_IN_PLAZA)

        return apiCall {
            apiClient.leavePlaza(plazaId)
            _activePlaza.value = null
        }
    }

    override suspend fun sendMessage(message: String): PlazaOperationResult<PlazaChatMessage> {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty()) {
            return PlazaOperationResult.Failure(PlazaOperationFailure.EMPTY_MESSAGE)
        }
        if (trimmedMessage.length > PLAZA_MESSAGE_MAX_LENGTH) {
            return PlazaOperationResult.Failure(PlazaOperationFailure.MESSAGE_TOO_LONG)
        }

        val currentPlaza = _activePlaza.value
            ?: return PlazaOperationResult.Failure(PlazaOperationFailure.NOT_IN_PLAZA)

        return apiCall {
            val response = apiClient.sendMessage(
                plazaId = currentPlaza.plazaId,
                request = SendPlazaMessageRequestDto(text = trimmedMessage)
            )
            val sentMessage = response.message?.toDomain(plazaId = currentPlaza.plazaId)
                ?: throw PlazaApiException(code = "EMPTY_RESPONSE")
            _activePlaza.update { plaza ->
                plaza?.copy(
                    messages = (plaza.messages.filterNot { it.id == sentMessage.id } + sentMessage)
                        .takeLast(50),
                    roomRevision = response.roomRevision ?: (plaza.roomRevision + 1L)
                )
            }
            sentMessage
        }
    }

    private suspend fun <T> apiCall(block: suspend () -> T): PlazaOperationResult<T> {
        return try {
            PlazaOperationResult.Success(block())
        } catch (exception: PlazaApiException) {
            Log.w(
                TAG,
                "Plaza API failed: code=${exception.code}, http=${exception.httpStatus}, message=${exception.message}"
            )
            PlazaOperationResult.Failure(exception.toPlazaOperationFailure())
        } catch (exception: IOException) {
            Log.w(TAG, "Plaza network failed.", exception)
            PlazaOperationResult.Failure(PlazaOperationFailure.NETWORK_ERROR)
        } catch (exception: JsonParseException) {
            Log.w(TAG, "Plaza response parse failed.", exception)
            PlazaOperationResult.Failure(PlazaOperationFailure.RESPONSE_ERROR)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Unexpected plaza failure.", exception)
            PlazaOperationResult.Failure(PlazaOperationFailure.UNKNOWN)
        }
    }

    private companion object {
        const val TAG = "LupaPlaza"
    }
}
