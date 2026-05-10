package com.example.lupapj.data.repository

import com.example.lupapj.data.model.plaza.PlazaChatMessage
import com.example.lupapj.data.model.plaza.PlazaOperationResult
import com.example.lupapj.data.model.plaza.PlazaPetSnapshot
import com.example.lupapj.data.model.plaza.PlazaRoom
import kotlinx.coroutines.flow.StateFlow

interface PlazaRepository {
    val activePlaza: StateFlow<PlazaRoom?>

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
