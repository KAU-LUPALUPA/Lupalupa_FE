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
        val CURRENCY_AMOUNT = longPreferencesKey("currency_amount_v2")
        val PURCHASED_ITEMS = stringPreferencesKey("purchased_items_v2") // instanceId:masterId 콤마 구분 문자열로 저장
    }

    // [추가됨(권)] 저장된 재화를 Flow로 관찰
    val currencyAmountFlow: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.CURRENCY_AMOUNT] ?: 0L // 초기 지급값 0
    }

    // [추가됨(권)] 저장된 인벤토리(구매 아이템 목록)를 Flow로 관찰
    val purchasedItemsFlow: Flow<List<com.example.lupapj.data.model.InventoryItem>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.PURCHASED_ITEMS] ?: ""
        if (raw.isEmpty()) emptyList() else raw.split(",").mapNotNull {
            val parts = it.split(":")
            if (parts.size >= 2) {
                com.example.lupapj.data.model.InventoryItem(
                    instanceId = parts[0],
                    masterId = parts[1],
                    count = parts.getOrNull(2)?.toIntOrNull() ?: 1
                )
            } else null
        }
    }

    // [추가됨(권)] 서버 응답 성공 후 재화를 캐시에 저장
    suspend fun saveCurrencyAmount(amount: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_AMOUNT] = amount
        }
    }

    // [추가됨(권)] 서버 응답 성공 후 인벤토리를 캐시에 저장
    suspend fun savePurchasedItems(items: List<com.example.lupapj.data.model.InventoryItem>) {
        dataStore.edit { prefs ->
            prefs[Keys.PURCHASED_ITEMS] = items.joinToString(",") { "${it.instanceId}:${it.masterId}:${it.count}" }
        }
    }
}
