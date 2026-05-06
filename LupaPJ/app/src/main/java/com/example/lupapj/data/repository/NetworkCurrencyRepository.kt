package com.example.lupapj.data.repository

import com.example.lupapj.data.local.ShopLocalCache
import com.example.lupapj.data.model.CurrencyState
import com.example.lupapj.data.model.CurrencyUpdateResult
import com.example.lupapj.data.remote.CurrencyRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [추가됨(권)] 분리된 RemoteDataSource를 사용하여 서버와 통신하고,
 * LocalCache(DataStore)를 관찰하여 UI에 실시간으로 상태를 전달하는 리포지토리입니다.
 */
class NetworkCurrencyRepository(
    private val remoteDataSource: CurrencyRemoteDataSource,
    private val localCache: ShopLocalCache
) : CurrencyRepository {

    private val _currencyState = MutableStateFlow(CurrencyState())
    override val currencyState: StateFlow<CurrencyState> = _currencyState.asStateFlow()

    init {
        // [추가됨(권)] Single Source of Truth: 로컬 캐시를 Flow로 관찰
        // DataStore에 값이 저장될 때마다 이 블록이 반응하여 StateFlow를 갱신합니다.
        CoroutineScope(Dispatchers.IO).launch {
            localCache.currencyAmountFlow.collect { amount ->
                _currencyState.update { it.copy(amount = amount) }
            }
        }
    }

    override suspend fun earnCurrency(amount: Long, source: String): CurrencyUpdateResult {
        // 1. 클라이언트에서 예상 잔액 계산 (기존 잔액 + 획득량)
        val currentTotal = _currencyState.value.amount
        val expectedTotal = currentTotal + amount

        // 2. 서버에 검증 및 반영 요청
        val result = remoteDataSource.earn(amount, expectedTotal, source)

        // 3. 서버 검증 성공 시에만 로컬 캐시에 저장
        if (result is CurrencyUpdateResult.Success) {
            localCache.saveCurrencyAmount(result.finalBalance)
            // (참고) 여기서 _currencyState를 직접 갱신하지 않아도,
            // init 블록의 collect가 DataStore 변경을 감지하고 알아서 UI를 갱신합니다.
        }

        return result
    }

    override suspend fun spendCurrency(amount: Long): CurrencyUpdateResult {
        val currentAmount = _currencyState.value.amount
        if (currentAmount >= amount) {
            val newAmount = currentAmount - amount
            // TODO: 추후 spend API가 백엔드에 추가되면 RemoteDataSource 호출 로직으로 변경
            localCache.saveCurrencyAmount(newAmount)
            return CurrencyUpdateResult.Success(newAmount)
        }
        return CurrencyUpdateResult.ValidationError("잔액이 부족합니다.")
    }
}
