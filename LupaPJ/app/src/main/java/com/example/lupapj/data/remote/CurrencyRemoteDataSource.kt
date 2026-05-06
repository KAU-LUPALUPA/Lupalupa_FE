package com.example.lupapj.data.remote

import com.example.lupapj.data.model.CurrencyUpdateResult
import com.example.lupapj.data.remote.model.CurrencyEarnRequest

/**
 * [추가됨(권)] 네트워크 통신을 통해 재화 데이터를 가져오는 DataSource.
 * Repository가 '어떻게' 데이터를 가져오는지 알 필요 없도록 캡슐화합니다.
 */
class CurrencyRemoteDataSource(
    private val apiService: CurrencyApiService
) {
    suspend fun earn(amount: Long, total: Long, source: String): CurrencyUpdateResult {
        return try {
            val request = CurrencyEarnRequest(amount = amount, total = total, source = source)
            val response = apiService.earnCurrency(request)

            when {
                response.isSuccessful -> {
                    // 서버가 성공을 반환하면, 우리가 계산한 total이 맞다는 의미
                    CurrencyUpdateResult.Success(total)
                }
                response.code() == 400 -> {
                    // 서버 검증 실패 (DB 잔액 + 획득량 != 예상 잔액)
                    CurrencyUpdateResult.ValidationError("검증 에러: 데이터 부정합 감지")
                }
                response.code() == 401 || response.code() == 403 -> {
                    // 인증 오류
                    CurrencyUpdateResult.AuthError
                }
                else -> {
                    CurrencyUpdateResult.NetworkError(Exception("Server returned code: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            CurrencyUpdateResult.NetworkError(e)
        }
    }
}
