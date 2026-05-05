package com.example.lupapj.data.network

import retrofit2.http.Body
import retrofit2.http.POST

// [추가됨] 재화 관련 API 엔드포인트를 정의하는 Retrofit 서비스 인터페이스.
interface CurrencyApiService {

    /**
     * 재화 획득을 서버에 동기화합니다.
     *
     * @param request 획득량(amount)과 획득 후 총 자산(total)을 포함하는 요청 객체
     * @return 서버 처리 결과 (status: "success" 또는 "fail")
     */
    @POST("currency/earn")
    suspend fun earnCurrency(@Body request: EarnCurrencyRequest): EarnCurrencyResponse
}

// [추가됨] POST /currency/earn 요청 본문
data class EarnCurrencyRequest(
    val amount: Int, // 획득량
    val total: Int   // 획득 후 예상 총 자산
)

// [추가됨] POST /currency/earn 응답 본문
data class EarnCurrencyResponse(
    val status: String // "success" 또는 "fail"
) {
    val isSuccess: Boolean get() = status == "success"
}
