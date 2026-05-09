package com.example.lupapj.data.repository

import com.example.lupapj.data.model.InventoryItem
import com.example.lupapj.data.model.ShopItem
import kotlinx.coroutines.flow.StateFlow

// [추가됨(권)] 상점 아이템 조회 및 구매를 처리하는 API 인터페이스
interface ShopRepository {
    // [추가됨(권)] 상점에서 판매 중인 전체 아이템 목록 상태
    val shopItems: StateFlow<List<ShopItem>>
    
    // [추가됨(권)] 사용자가 이미 구매하여 보유하고 있는 아이템 목록 상태 (인스턴스 ID 포함)
    val inventory: StateFlow<List<InventoryItem>>

    /**
     * [추가됨(권)] 상점 목록을 서버로부터 조회하여 shopItems 상태를 갱신합니다.
     */
    suspend fun fetchShopItems()

    /**
     * [추가됨(권)] 아이템을 구매합니다. 내부적으로 CurrencyRepository를 통해 재화 소모를 검증하며, 성공 시 inventory에 아이템을 추가합니다.
     */
    suspend fun purchaseItem(itemId: String): Result<Unit>
}
