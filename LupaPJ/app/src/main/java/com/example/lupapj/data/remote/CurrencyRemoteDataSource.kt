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
            // [수정됨(권)] 서버 GoldRequest 스펙에 맞춰 source 필드 제외 및 타입 캐스팅(Int)
            val request = CurrencyEarnRequest(amount = amount.toInt(), total = total)
            val response = apiService.earnCurrency(request)

            if (response.isSuccessful) {
                val body = response.body()
                // [수정됨(권)] status가 "success"인지 확인하는 로직 추가
                if (body?.status == "success") {
                    CurrencyUpdateResult.Success(body.total ?: total)
                } else {
                    CurrencyUpdateResult.ValidationError(
                        message = "서버 검증 실패 (Status: ${body?.status})",
                        correctBalance = body?.total
                    )
                }
            } else {
                when (response.code()) {
                    400 -> {
                        // [수정됨(권)] 서버 400 에러 시 에러 바디에서 현재 서버 잔액(total)을 파싱하여 강제 동기화에 사용
                        val errorBody = response.errorBody()?.string()
                        val errorDto = try {
                            gson.fromJson(errorBody, CurrencyResponseDto::class.java)
                        } catch (e: Exception) {
                            null
                        }
                        
                        CurrencyUpdateResult.ValidationError(
                            message = "재화 검증 실패: 서버 잔액과 일치하지 않습니다.",
                            correctBalance = errorDto?.total
                        )
                    }
                    401, 403 -> CurrencyUpdateResult.AuthError
                    else -> CurrencyUpdateResult.NetworkError(Exception("Server Error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            CurrencyUpdateResult.NetworkError(e)
        }
    }
}
