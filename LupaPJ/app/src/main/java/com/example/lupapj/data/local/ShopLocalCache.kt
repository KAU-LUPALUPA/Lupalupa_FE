package com.example.lupapj.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// [추가됨(권)] 재화와 인벤토리 상태를 로컬 디스크에 캐싱하기 위한 DataStore 래퍼 클래스.
// 서버(Mock) 응답이 성공한 뒤에만 이 캐시에 기록하므로, 로컬 변조로 서버 데이터를 오염시킬 수 없습니다.
// 로그인 시 서버에서 최신 값을 받아 이 캐시를 덮어쓰는 방식으로 동기화합니다.

// [추가됨(권)] Context 확장 프로퍼티로 DataStore 인스턴스를 싱글톤 생성
private val Context.shopDataStore: DataStore<Preferences> by preferencesDataStore(name = "shop_cache")

class ShopLocalCache(context: Context) {

    private val dataStore = context.applicationContext.shopDataStore

    // [추가됨(권)] DataStore 키 정의
    private object Keys {
        val CURRENCY_AMOUNT = intPreferencesKey("currency_amount")
        val PURCHASED_ITEM_IDS = stringPreferencesKey("purchased_item_ids") // 콤마 구분 문자열로 저장
    }

    // [추가됨(권)] 저장된 재화를 Flow로 관찰
    val currencyAmountFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.CURRENCY_AMOUNT] ?: 100 // 초기 지급값 100
    }

    // [추가됨(권)] 저장된 인벤토리(구매 아이템 ID 목록)를 Flow로 관찰
    val purchasedItemIdsFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.PURCHASED_ITEM_IDS] ?: ""
        if (raw.isEmpty()) emptyList() else raw.split(",")
    }

    // [추가됨(권)] 서버 응답 성공 후 재화를 캐시에 저장
    suspend fun saveCurrencyAmount(amount: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_AMOUNT] = amount
        }
    }

    // [추가됨(권)] 서버 응답 성공 후 인벤토리를 캐시에 저장
    suspend fun savePurchasedItemIds(ids: List<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.PURCHASED_ITEM_IDS] = ids.joinToString(",")
        }
    }
}
