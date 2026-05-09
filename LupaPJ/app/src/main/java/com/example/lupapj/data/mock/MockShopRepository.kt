package com.example.lupapj.data.mock

import com.example.lupapj.data.local.ShopLocalCache
import com.example.lupapj.data.model.InventoryItem
import com.example.lupapj.data.model.ShopCategory
import com.example.lupapj.data.model.ShopItem
import com.example.lupapj.data.repository.CurrencyRepository
import com.example.lupapj.data.repository.ShopRepository
import java.util.UUID // [추가됨]
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import com.example.lupapj.data.model.DefaultShopItems // [추가됨]

// [추가됨(권)] 상점 기능(목록 조회 및 구매)의 서버 통신을 모사하는 Mock 객체.
class MockShopRepository(
    private val currencyRepository: CurrencyRepository,
    private val localCache: ShopLocalCache 
) : ShopRepository {

    private val _shopItems = MutableStateFlow<List<ShopItem>>(emptyList())
    override val shopItems: StateFlow<List<ShopItem>> = _shopItems.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    override val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    init {
        _shopItems.value = DefaultShopItems

        CoroutineScope(Dispatchers.IO).launch {
            val cachedItems = localCache.purchasedItemsFlow.first()
            _inventory.value = cachedItems
        }
    }

    override suspend fun fetchShopItems() {
        delay(500)
        _shopItems.value = DefaultShopItems
    }

    override suspend fun purchaseItem(itemId: String): Result<Unit> {
        delay(500)
        val item = DefaultShopItems.find { it.id == itemId } ?: return Result.failure(Exception("아이템을 찾을 수 없습니다."))
        
        // [추가됨(권)] 재화 소모 검증
        val spendResult = currencyRepository.spendCurrency(item.price.toLong())
        return if (spendResult is com.example.lupapj.data.model.CurrencyUpdateResult.Success) {
            
            // [추가됨(권)] 인벤토리 업데이트 (겹치기 가능한 경우 기존 항목 count 증가)
            val existingItem = _inventory.value.find { it.masterId == itemId }
            val newInventory = if (existingItem != null) {
                // 이미 보유한 경우 수량만 증가 (겹치기 로직)
                _inventory.value.map {
                    if (it.masterId == itemId) it.copy(count = it.count + 1) else it
                }
            } else {
                // 새로 구매하는 경우 새로운 인스턴스 ID 발급 (uuid + 랜덤 10자리)
                val randomDigits = (0 until 10).map { Random.nextInt(10) }.joinToString("")
                val newInstanceId = "${UUID.randomUUID()}$randomDigits"
                val newItem = InventoryItem(instanceId = newInstanceId, masterId = itemId, count = 1)
                _inventory.value + newItem
            }
            
            _inventory.value = newInventory
            localCache.savePurchasedItems(newInventory)
            Result.success(Unit)
        } else {
            val errorMessage = if (spendResult is com.example.lupapj.data.model.CurrencyUpdateResult.ValidationError) spendResult.message else "재화가 부족합니다."
            Result.failure(Exception(errorMessage))
        }
    }
}
