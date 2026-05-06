package com.example.lupapj.data.model

/**
 * [추가됨(권)] 재화 업데이트(획득/소모) 요청의 상세 결과를 나타내는 Sealed Class.
 * 단순 성공/실패(Boolean)를 넘어, 에러의 원인을 UI에 명확히 전달하기 위해 사용합니다.
 */
sealed class CurrencyUpdateResult {
    data class Success(val finalBalance: Long) : CurrencyUpdateResult()
    data class ValidationError(val message: String) : CurrencyUpdateResult()
    data class NetworkError(val exception: Throwable) : CurrencyUpdateResult()
    object AuthError : CurrencyUpdateResult()
}
