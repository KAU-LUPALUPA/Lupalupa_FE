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

        // 3. 서버 응답 처리
        when (result) {
            is CurrencyUpdateResult.Success -> {
                // 검증 성공: 서버와 클라이언트의 계산이 일치함
                localCache.saveCurrencyAmount(result.finalBalance)
            }
            is CurrencyUpdateResult.ValidationError -> {
                // [추가됨] 검증 실패했더라도 서버가 올바른 잔액을 보내줬다면 강제로 동기화
                result.correctBalance?.let {
                    localCache.saveCurrencyAmount(it)
                }
            }
            else -> { /* 네트워크 에러 등은 로컬 캐시를 건드리지 않음 */ }
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

    override suspend fun syncCurrency(amount: Long) {
        // [추가됨(권)] 서버에서 내려준 확정 잔액으로 로컬 캐시 동기화
        localCache.saveCurrencyAmount(amount)
    }
}
