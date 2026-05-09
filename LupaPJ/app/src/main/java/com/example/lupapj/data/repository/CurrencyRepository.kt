package com.example.lupapj.data.repository

import com.example.lupapj.data.model.CurrencyState
import com.example.lupapj.data.model.CurrencyUpdateResult
import kotlinx.coroutines.flow.StateFlow

// [추가됨(권)] 재화 관리 API 인터페이스. 로컬 상태 관리 및 서버 통신(재화 증감)을 추상화.
interface CurrencyRepository {
    // [추가됨(권)] 현재 재화 상태를 관찰할 수 있는 StateFlow
    val currencyState: StateFlow<CurrencyState>

    /**
     * [추가됨(권)] 재화 획득을 요청하고 검증합니다 (백엔드 연동)
     * @param amount 이번에 획득한 양
     * @param source 획득 경로 (추후 검증을 위해 추가)
     */
    suspend fun earnCurrency(amount: Long, source: String = "MINIGAME"): CurrencyUpdateResult

    /**
     * [추가됨(권)] 재화 소모를 요청하고 검증합니다 (백엔드 연동)
     */
    suspend fun spendCurrency(amount: Long): CurrencyUpdateResult

    /**
     * [추가됨(권)] 서버에서 내려준 정확한 잔액으로 로컬 상태를 강제 동기화합니다.
     */
    suspend fun syncCurrency(amount: Long)
}
