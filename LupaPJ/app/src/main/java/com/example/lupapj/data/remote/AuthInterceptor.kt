package com.example.lupapj.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * [추가됨(권)] 서버 통신 시 JWT 토큰을 자동으로 헤더에 주입하는 Interceptor.
 * 인증 관련 로직을 한 곳에서 모아 관리(Maintainability)하기 위함입니다.
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()
        
        // S3로 가는 요청(presigned url)에는 Authorization 헤더를 추가하지 않음
        if (!request.url.host.contains("amazonaws.com")) {
            // 토큰이 존재하면 Authorization 헤더 추가
            val token = tokenProvider()
            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
        }
        
        return chain.proceed(requestBuilder.build())
    }
}
