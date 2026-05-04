package com.example.lupapj.data.repository

import com.example.lupapj.data.model.CurrencyState
import kotlinx.coroutines.flow.StateFlow

// [추가됨(권)] 재화 관리 API 인터페이스. 로컬 상태 관리 및 서버 통신(재화 증감)을 추상화.
interface CurrencyRepository {
    // [추가됨(권)] 현재 재화 상태를 관찰할 수 있는 StateFlow
    val currencyState: StateFlow<CurrencyState>

    /**
     * [추가됨(권)] 재화 획득을 요청하고 검증합니다 (백엔드 연동)
     */
    suspend fun earnCurrency(amount: Int): Boolean

    /**
     * [추가됨(권)] 재화 소모를 요청하고 검증합니다 (백엔드 연동)
     */
    suspend fun spendCurrency(amount: Int): Boolean
}
