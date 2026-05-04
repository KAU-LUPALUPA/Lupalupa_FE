package com.example.lupapj.data.mock

import com.example.lupapj.data.local.ShopLocalCache
import com.example.lupapj.data.model.CurrencyState
import com.example.lupapj.data.repository.CurrencyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// [추가됨(권)] 재화 API 서버 통신을 모사하는 Mock 객체.
// DataStore 로컬 캐시에서 초기값을 불러오고, 서버(Mock) 응답 성공 후에만 캐시를 갱신합니다.
class MockCurrencyRepository(
    private val localCache: ShopLocalCache // [추가됨(권)] 로컬 캐시 주입
) : CurrencyRepository {

    private val _currencyState = MutableStateFlow(CurrencyState(amount = 100))
    override val currencyState: StateFlow<CurrencyState> = _currencyState.asStateFlow()

    init {
        // [추가됨(권)] 앱 시작 시 DataStore에 저장된 마지막 서버 응답값을 불러와서 초기 상태로 설정
        CoroutineScope(Dispatchers.IO).launch {
            val cachedAmount = localCache.currencyAmountFlow.first()
            _currencyState.value = CurrencyState(amount = cachedAmount)
        }
    }

    override suspend fun earnCurrency(amount: Int): Boolean {
        delay(300) // [추가됨(권)] 네트워크 통신 지연 시뮬레이션
        val newAmount = _currencyState.value.amount + amount
        _currencyState.update { it.copy(amount = newAmount) }
        // [추가됨(권)] 서버(Mock) 응답 성공 후 로컬 캐시에 기록
        localCache.saveCurrencyAmount(newAmount)
        return true
    }

    override suspend fun spendCurrency(amount: Int): Boolean {
        delay(300) // [추가됨(권)] 네트워크 통신 지연 시뮬레이션
        val currentAmount = _currencyState.value.amount
        // [추가됨(권)] 서버 단에서 잔액을 검증하는 과정을 모사. 로컬 재화가 요구량보다 많거나 같을 때만 차감 진행
        if (currentAmount >= amount) {
            val newAmount = currentAmount - amount
            _currencyState.update { it.copy(amount = newAmount) }
            // [추가됨(권)] 서버(Mock) 응답 성공 후 로컬 캐시에 기록
            localCache.saveCurrencyAmount(newAmount)
            return true
        }
        return false // [추가됨(권)] 잔액 부족: 서버 측 구매 검증 실패 시뮬레이션
    }
}
