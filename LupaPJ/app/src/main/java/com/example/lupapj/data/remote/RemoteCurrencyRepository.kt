package com.example.lupapj.data.remote

import android.util.Log
import com.example.lupapj.data.local.ShopLocalCache
import com.example.lupapj.data.model.CurrencyState
import com.example.lupapj.data.network.CurrencyApiService
import com.example.lupapj.data.network.EarnCurrencyRequest
import com.example.lupapj.data.repository.CurrencyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "RemoteCurrencyRepo"

// [추가됨] 실제 서버와 통신하는 CurrencyRepository 구현체.
// 서버 응답이 성공한 경우에만 로컬 캐시를 업데이트합니다 (서버 권위 모델).
class RemoteCurrencyRepository(
    private val apiService: CurrencyApiService,
    private val localCache: ShopLocalCache
) : CurrencyRepository {

    private val _currencyState = MutableStateFlow(CurrencyState(amount = 100))
    override val currencyState: StateFlow<CurrencyState> = _currencyState.asStateFlow()

    init {
        // 앱 시작 시 DataStore에 저장된 마지막 서버 동기화값을 불러와서 초기 상태로 설정
        CoroutineScope(Dispatchers.IO).launch {
            val cachedAmount = localCache.currencyAmountFlow.first()
            _currencyState.value = CurrencyState(amount = cachedAmount)
        }
    }

    /**
     * 재화 획득 후 서버에 동기화합니다.
     * 1. 로컬에서 먼저 획득 후 총액을 계산합니다.
     * 2. POST /currency/earn 으로 { amount, total } 을 서버에 전송합니다.
     * 3. 서버가 "success"를 반환하면 로컬 상태와 캐시를 업데이트합니다.
     * 4. 실패 시 로컬 상태를 롤백합니다.
     */
    override suspend fun earnCurrency(amount: Int): Boolean {
        val currentAmount = _currencyState.value.amount
        val expectedTotal = currentAmount + amount

        return try {
            val response = apiService.earnCurrency(
                EarnCurrencyRequest(
                    amount = amount,
                    total = expectedTotal
                )
            )

            if (response.isSuccess) {
                // 서버 확인 완료 → 로컬 상태 및 캐시 업데이트
                _currencyState.update { it.copy(amount = expectedTotal) }
                localCache.saveCurrencyAmount(expectedTotal)
                Log.d(TAG, "재화 획득 성공: +$amount → 총 $expectedTotal")
                true
            } else {
                // 서버가 fail을 반환한 경우
                Log.w(TAG, "재화 획득 실패: 서버가 fail 응답 반환")
                false
            }
        } catch (e: Exception) {
            // 네트워크 오류 등 예외 발생 시
            Log.e(TAG, "재화 획득 실패: 네트워크 오류", e)
            false
        }
    }

    /**
     * 재화 소비. 현재는 로컬 검증만 수행합니다.
     * TODO: 추후 서버 API가 추가되면 spendCurrency도 서버 연동합니다.
     */
    override suspend fun spendCurrency(amount: Int): Boolean {
        val currentAmount = _currencyState.value.amount
        if (currentAmount >= amount) {
            val newAmount = currentAmount - amount
            _currencyState.update { it.copy(amount = newAmount) }
            localCache.saveCurrencyAmount(newAmount)
            return true
        }
        return false
    }
}
