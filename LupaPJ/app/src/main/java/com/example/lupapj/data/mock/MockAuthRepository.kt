package com.example.lupapj.data.mock

import com.example.lupapj.data.model.AuthSession
import com.example.lupapj.data.repository.AuthRepository
import kotlinx.coroutines.delay

class MockAuthRepository : AuthRepository {
    override suspend fun loginWithKakao(kakaoAccessToken: String): AuthSession {
        delay(450)
        return AuthSession(userId = "mock-kakao-user")
    }
}
