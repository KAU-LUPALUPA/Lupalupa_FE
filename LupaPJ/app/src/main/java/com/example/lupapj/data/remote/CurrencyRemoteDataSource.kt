package com.example.lupapj.data.remote

import com.example.lupapj.data.model.CurrencyUpdateResult
import com.example.lupapj.data.remote.model.CurrencyEarnRequest
import com.example.lupapj.data.remote.model.CurrencyResponseDto
import com.google.gson.Gson

/**
 * [추가됨(권)] 네트워크 통신을 통해 재화 데이터를 가져오는 DataSource.
 */
class CurrencyRemoteDataSource(
    private val apiService: CurrencyApiService
) {
    private val gson = Gson()

    suspend fun earn(amount: Long, total: Long, source: String): CurrencyUpdateResult {
        return try {
            val request = CurrencyEarnRequest(amount = amount, total = total, source = source)
            val response = apiService.earnCurrency(request)

            if (response.isSuccessful) {
                val body = response.body()
                // 서버가 보내준 최신 잔액이 있으면 그것을 사용, 없으면 요청했던 total 사용
                CurrencyUpdateResult.Success(body?.currentBalance ?: total)
            } else {
                when (response.code()) {
                    400 -> {
                        // [수정됨] 서버 검증 실패 시 에러 바디에서 현재 잔액 추출
                        val errorBody = response.errorBody()?.string()
                        val errorDto = try {
                            gson.fromJson(errorBody, CurrencyResponseDto::class.java)
                        } catch (e: Exception) {
                            null
                        }
                        CurrencyUpdateResult.ValidationError(
                            message = "데이터 부정합 감지 (클라이언트 예상: $total, 서버 잔액: ${errorDto?.currentBalance ?: "N/A"})",
                            correctBalance = errorDto?.currentBalance
                        )
                    }
                    401, 403 -> CurrencyUpdateResult.AuthError
                    else -> CurrencyUpdateResult.NetworkError(Exception("Server returned code: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            CurrencyUpdateResult.NetworkError(e)
        }
    }
}
