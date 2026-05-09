package com.example.lupapj.data.remote

import com.example.lupapj.data.remote.model.CurrencyEarnRequest
import com.example.lupapj.data.remote.model.CurrencyResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * [추가됨(권)] 재화 관련 백엔드 API 인터페이스
 */
interface CurrencyApiService {

    /**
     * 재화 획득 및 검증 요청
     */
    @POST("currency/earn")
    suspend fun earnCurrency(
        @Body request: CurrencyEarnRequest
    ): Response<CurrencyResponseDto>
}
