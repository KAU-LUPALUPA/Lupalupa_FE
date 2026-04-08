package com.example.lupapj.data.repository

import com.example.lupapj.data.model.AuthSession

interface AuthRepository {
    suspend fun loginWithKakao(kakaoAccessToken: String): AuthSession
}
