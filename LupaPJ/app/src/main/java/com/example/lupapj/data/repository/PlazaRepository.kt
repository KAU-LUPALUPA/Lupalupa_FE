package com.example.lupapj.data.repository

import com.example.lupapj.data.model.plaza.PlazaChatMessage
import com.example.lupapj.data.model.plaza.PlazaOperationFailure
import com.example.lupapj.data.model.plaza.PlazaOperationResult
import com.example.lupapj.data.model.plaza.PlazaPetSnapshot
import com.example.lupapj.data.model.plaza.PlazaRoom
import kotlinx.coroutines.flow.StateFlow

interface PlazaRepository {
    val activePlaza: StateFlow<PlazaRoom?>

    fun updateCurrentUser(userId: String?, nickname: String?) = Unit

    suspend fun refreshActivePlaza(): PlazaOperationResult<PlazaRoom?> {
        return PlazaOperationResult.Success(activePlaza.value)
    }

    suspend fun refreshPlazaSnapshot(plazaId: String): PlazaOperationResult<PlazaRoom> {
        val plaza = activePlaza.value ?: return PlazaOperationResult.Failure(
            PlazaOperationFailure.NOT_IN_PLAZA
        )
        return if (plaza.plazaId == plazaId) {
            PlazaOperationResult.Success(plaza)
        } else {
            PlazaOperationResult.Failure(PlazaOperationFailure.PLAZA_NOT_FOUND)
        }
    }

    suspend fun joinRandomPlaza(
        currentUserId: String,
        nickname: String,
        pet: PlazaPetSnapshot
    ): PlazaOperationResult<PlazaRoom>

    suspend fun joinPlazaByCode(
        codeInput: String,
        currentUserId: String,
        nickname: String,
        pet: PlazaPetSnapshot
    ): PlazaOperationResult<PlazaRoom>

    suspend fun leavePlaza(): PlazaOperationResult<Unit>

    suspend fun sendMessage(message: String): PlazaOperationResult<PlazaChatMessage>
}
